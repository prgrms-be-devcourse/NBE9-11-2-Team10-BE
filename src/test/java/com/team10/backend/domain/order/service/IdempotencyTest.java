package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.ConfirmRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.repository.IdempotencyRepository;
import com.team10.backend.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.team10.backend.global.exception.ErrorCode.ALREADY_PROCESSED_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class IdempotencyTest {
    @Autowired
    private OrderConfirmService orderConfirmService; // sendConfirmRequest가 포함된 서비스

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private RestTemplate restTemplate; // 외부 API 호출은 Mocking

    @BeforeEach
    void cleanUp() {
        idempotencyRepository.deleteAll();
    }

    @Test
    @DisplayName("10개의 스레드가 동시에 결제 승인을 요청하면 오직 1번만 성공해야 한다")
    void concurrencyTest() throws InterruptedException {
        // given
        String orderId = "ORDER_" + UUID.randomUUID();
        ConfirmRequest request = new ConfirmRequest("paymentKey",orderId ,15000L);

        // 가짜 성공 응답 설정 (첫 번째 진입 스레드용)
        TossConfirmResponse mockResponse = new TossConfirmResponse("paymentKey",orderId, "DONE");
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads); // 모든 스레드가 준비될 때까지 대기

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    orderConfirmService.sendConfirmRequest(request, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {

                    // 만약 Retry가 개입했다면 원본 에러(getCause)를 추출
                    Throwable actualException = e;
                    if (e instanceof org.springframework.retry.ExhaustedRetryException) {
                        actualException =e.getCause();
                    }

                    // 2. 예외 타입 및 에러 코드 검증
                    if (actualException instanceof BusinessException) {
                        BusinessException be = (BusinessException) actualException;
                        if (be.getErrorCode().equals(ALREADY_PROCESSED_PAYMENT)) {
                            errorMessages.add(be.getMessage());
                            failCount.incrementAndGet();
                        } else {
                            log.error("예상치 못한 비즈니스 에러: {}", be.getErrorCode());
                            failCount.incrementAndGet();
                        }
                    } else {
                        log.error("비즈니스 예외가 아닌 에러 발생: ", e);
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기
        executorService.shutdown();

        // then
        // 1. 성공은 딱 1번만 발생해야 함
        assertThat(successCount.get()).isEqualTo(1);

        // 2. 실패 에러는 총 9번 발생해야 함
        assertThat(failCount.get()).isEqualTo(numberOfThreads - 1);


        // 3. SUCCESS로 최종 완료되어 있어야 함
        IdempotencyRecord record = idempotencyRepository.findByOrderId(orderId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.SUCCESS);
    }

    @Test
    @DisplayName("결제 성공 시 SUCCESS 상태로 변경되고 응답 데이터가 JSON으로 저장되어야 한다")
    void success_state_transition() {
        // given
        String orderId = "ORDER_SUCCESS_01";
        ConfirmRequest request = new ConfirmRequest("paymentKey", orderId, 15000L);
        TossConfirmResponse mockResponse = new TossConfirmResponse("paymentKey", orderId, "DONE");

        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // when
        orderConfirmService.sendConfirmRequest(request, null);

        // then
        IdempotencyRecord record = idempotencyRepository.findByOrderId(orderId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.SUCCESS);
        assertThat(record.getResponseBody()).contains("DONE");
    }

    @Test
    @DisplayName("4xx 비즈니스 에러 발생 시 FAILED 상태가 되고, 재요청 시 새로운 토스 키가 생성되어야 한다")
    void business_error_and_key_refresh() {
        // given
        String orderId = "ORDER_FAIL_01";
        ConfirmRequest request = new ConfirmRequest("paymentKey", orderId, 15000L);

        // 1. 첫 번째 시도: 400 Bad Request (잔액 부족 등)
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "잔액 부족"));

        // when (첫 번째 시도)
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, null);
        });

        // Retry에 의해 감싸져 있다면 원본 BusinessException 추출
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }
        assertTrue(actualException instanceof HttpClientErrorException, "발생한 예외는 HttpClientErrorException.");

        // then (첫 번째 실패 확인)
        IdempotencyRecord firstRecord = idempotencyRepository.findByOrderId(orderId).orElseThrow();
        String firstTossKey = firstRecord.getLastTossKey();
        assertThat(firstRecord.getStatus()).isEqualTo(IdempotencyStatus.FAILED);

        // 2. 두 번째 시도: 성공으로 설정
        TossConfirmResponse mockResponse = new TossConfirmResponse("paymentKey", orderId, "DONE");
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // when (두 번째 시도)
        orderConfirmService.sendConfirmRequest(request, null);

        // then (재시도 시 키 갱신 확인)
        IdempotencyRecord secondRecord = idempotencyRepository.findByOrderId(orderId).orElseThrow();
        assertThat(secondRecord.getStatus()).isEqualTo(IdempotencyStatus.SUCCESS);
        assertThat(secondRecord.getLastTossKey()).isNotEqualTo(firstTossKey); // 키가 갱신되었는지 확인
        assertThat(secondRecord.getLastTossKey()).startsWith(orderId + "_");
    }

    @Test
    @DisplayName("5xx 서버 에러 발생 시 FAILED 상태가 되어야 한다")
    void system_error_transition() {
        // given
        String orderId = "ORDER_5XX_01";
        ConfirmRequest request = new ConfirmRequest("paymentKey", orderId, 10000L);

        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "토스 서버 장애"));


        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, null);
        });
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }
        assertTrue(actualException instanceof HttpServerErrorException, "발생한 예외는 HttpServerErrorException.");

        // then
        IdempotencyRecord record = idempotencyRepository.findByOrderId(orderId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
    }

    @Test
    @DisplayName("이미 성공(SUCCESS)한 주문 ID로 요청 시, 외부 API를 호출하지 않고 캐시된 응답을 반환해야 한다")
    void cache_hit_test_no_external_call() {
        // given
        String orderId = "ORDER_ALREADY_SUCCESS_123";
        String savedJsonResponse = "{\"paymentKey\":\"key_123\",\"orderId\":\"ORDER_ALREADY_SUCCESS_123\",\"status\":\"DONE\"}";

        // 1. 이미 성공한 레코드를 DB에 미리 저장
        IdempotencyRecord record = new IdempotencyRecord(orderId, "toss_key_init");
        record.complete(savedJsonResponse); // SUCCESS 상태로 설정
        idempotencyRepository.saveAndFlush(record);

        ConfirmRequest request = new ConfirmRequest("key_123", orderId, 15000L);

        // when
        TossConfirmResponse response = orderConfirmService.sendConfirmRequest(request, null);

        // then
        // 1. 반환된 응답값이 DB에 저장되어 있던 값과 일치하는지 확인
        assertThat(response).isNotNull();
        assertThat(  response.orderId()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo("DONE");

        // 2. RestTemplate의 postForEntity 메서드가 한 번도 호출되지 않았음을 검증
        verify(restTemplate, times(0))
                .postForEntity(anyString(), any(), eq(TossConfirmResponse.class));

        log.info("외부 API 호출 없이 DB 데이터를 반환했습니다.");
    }
}

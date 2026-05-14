package com.team10.backend.domain.order.service;


import com.team10.backend.domain.order.dto.confirm.ConfirmRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.enums.PaymentStatus;
import com.team10.backend.domain.order.repository.OrderRepository;
import com.team10.backend.domain.order.repository.PaymentRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.team10.backend.global.exception.ErrorCode.ALREADY_PROCESSED_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class PaymentStatusServiceTest {
    @Autowired
    private OrderConfirmService orderConfirmService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        // 1. 유저 및 상품 세팅
        insertUser(1L, "buyer@test.com", "홍길동", "nickname1", "BUYER");
        insertUser(2L, "seller@test.com", "홍길동2", "nickname2", "SELLER");
        insertProduct(101L, 2L, "상품A", 15000);

        // 2. 승인 테스트를 위한 주문 생성 (상태: PENDING)
        insertOrder(500L, 1L, "ORD-SUCCESS-100", 15000, "2026-04-29 10:00:00");
    }

    // ================= SQL 집중 관리 영역 =================

    private void cleanupDatabase() {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_products");
        jdbcTemplate.update("DELETE FROM order_delivery");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");
    }

    private void insertUser(Long id, String email, String name, String nickname, String role) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, '1234', ?, ?, '010-0000-0000', '주소', 'ACTIVE', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, email, name,nickname, role
        );
    }

    private void insertProduct(Long id, Long userId, String name, int price) {
        jdbcTemplate.update(
                "INSERT INTO products (id, user_id, product_name, price, stock, type, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 10, 'BOOK', 'SELLING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, userId, name, price
        );
    }

    private void insertOrder(Long id, Long userId, String orderNum, int amount, String date) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, user_id, order_number, total_amount, status, is_deleted, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                userId,
                orderNum,
                amount,
                "PENDING", // status 기본값 (Enum의 문자열 값)
                0,     // is_deleted 기본값
                date,
                date
        );
    }



    // Payment2 테이블 구조에 맞게 수정된 인서트 메서드
    private void insertPayment(Long id, Long orderId, String orderNumber, int amount,
                                String status, String idemKey, String type) {
        jdbcTemplate.update(
                "INSERT INTO payments (id, order_id, order_number, total_amount, status, idempotency_key, type, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, orderId, orderNumber, amount, status, idemKey, type
        );
    }



    @Test
    @DisplayName("최초 결제 승인 성공 시, Payment2 상태가 PAID로 변경되고 응답이 저장되어야 한다")
    void success_first_payment_attempt() {
        // given
        String orderId = "ORD-SUCCESS-100";
        long amount = 15000L;
        String paymentKey = "toss_payment_key_123";

        // 토스 API 응답 Mocking
        TossConfirmResponse mockResponse = new TossConfirmResponse(paymentKey, orderId, "DONE");
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // 승인 요청 DTO (프론트에서 온 데이터 가정)
        ConfirmRequest request = new ConfirmRequest(paymentKey, orderId, amount);

        // when
        TossConfirmResponse result = orderConfirmService.sendConfirmRequest(request, null);

        // then
        // 1. 반환값 확인
        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.paymentKey()).isEqualTo(paymentKey);

        // 2. DB 상태 확인 (Payment2 레코드가 생성되었고 PAID인지)
        Payment savedPayment = paymentRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new AssertionError("Payment2 레코드가 생성되지 않았습니다."));

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getTotalAmount()).isEqualTo((int) amount);
        assertThat(savedPayment.getOrderNumber()).isEqualTo(orderId); // 최초 요청은 접미사 없음
        assertThat(savedPayment.getResponseBody()).isNotNull(); // 응답 데이터 저장 확인
    }
    @Test
    @DisplayName("이미 PAID 상태인 결제 기록이 존재하면, 토스 API를 호출하지 않고 저장된 응답을 반환한다")
    void duplicate_request_return_stored_response() {
        // given
        String orderId = "ORD-DUPLICATE-200";
        long amount = 30000L;
        String paymentKey = "already_success_key";
        String mockStoredResponse = "{\"paymentKey\":\"already_success_key\",\"orderId\":\"ORD-DUPLICATE-200\",\"status\":\"DONE\"}";

        // 1. 주문 데이터 삽입 (ID: 600L)
        insertOrder(600L, 1L, orderId, (int) amount, "2026-04-29 11:00:00");

        // 2. 이미 PAID 상태인 Payment2 데이터를 미리 삽입 (ID: 900L)
        // 멱등키와 응답 바디가 이미 저장되어 있는 상태를 시뮬레이션
        insertPayment(
                900L,
                600L,
                orderId,
                (int) amount,
                "PAID",
                "existing_idempotency_key",
                "PAYMENT"
        );

        // DB에 직접 response_body 업데이트 (JSON 문자열 저장)
        jdbcTemplate.update(
                "UPDATE payments SET response_body = ? WHERE id = ?",
                mockStoredResponse, 900L
        );

        // 승인 요청 DTO
        ConfirmRequest request = new ConfirmRequest(paymentKey, orderId, amount);

        // when
        TossConfirmResponse result = orderConfirmService.sendConfirmRequest(request, null);

        // then
        // 1. 결과값이 DB에 저장되어 있던 값과 일치하는지 확인
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo("DONE");

        // 2. [가장 중요] RestTemplate이 한 번도 호출되지 않았는지 검증
        verify(restTemplate, times(0)).postForEntity(anyString(), any(), any());

        // 3. 새로운 Payment 레코드가 생성되지 않았는지 확인 (여전히 1개여야 함)
        int paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE order_id = 600", Integer.class);
        assertThat(paymentCount).isEqualTo(1);
    }

//    @Test
//    @DisplayName("기존 결제가 FAILED 상태라면, 새로운 orderId(_v2)를 생성하여 승인을 시도한다")
//    void failed_then_retry_with_v2() {
//        // given
//        String originalOrderId = "ORD-FAILED-300";
//        long amount = 20000L;
//        String paymentKey = "new_attempt_key";
//
//        // 1. 주문 데이터 삽입 (ID: 700L)
//        insertOrder(700L, 1L, originalOrderId, (int) amount, "2026-04-29 12:00:00");
//
//        // 2. 이미 실패한(FAILED) Payment2 데이터 삽입 (ID: 950L) - 이게 v1 역할
//        insertPayment(
//                950L,
//                700L,
//                originalOrderId,
//                (int) amount,
//                "FAILED",
//                "failed_idempotency_key",
//                "PAYMENT"
//        );
//
//        // 3. 토스 API Mock 설정: 실제로는 ORD-FAILED-300_v2로 요청이 갈 것을 기대함
//        String expectedV2OrderId = originalOrderId + "_v1";
//        TossConfirmResponse mockResponse = new TossConfirmResponse(paymentKey, expectedV2OrderId, "DONE");
//
//        // ArgumentCaptor를 사용하여 실제 토스에 어떤 orderId가 전달되는지 캡처 준비
//        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
//        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
//                .thenReturn(ResponseEntity.ok(mockResponse));
//
//        // 승인 요청 DTO (사용자는 여전히 원래의 orderId를 보냄)
//        ConfirmRequest request = new ConfirmRequest(paymentKey, originalOrderId, amount);
//
//        // when
//        TossConfirmResponse result = orderConfirmService.sendConfirmRequest(request, null);
//
//        // then
//        // 1. 토스로 보낸 실제 Body 검증 (v2가 붙어있어야 함)
//        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(TossConfirmResponse.class));
//        ConfirmRequest capturedBody = (ConfirmRequest) entityCaptor.getValue().getBody();
//        assertThat(capturedBody.orderId()).isEqualTo(expectedV2OrderId);
//
//        // 2. DB 검증: Payment2 레코드가 이제 2개여야 함 (v1: FAILED, v2: PAID)
//        List<Payment2> payments = paymentRepository2.findAllByOrderOrderByCreatedAtAsc(
//                orderRepository.findByOrderNumber(originalOrderId).get()
//        );
//
//        assertThat(payments).hasSize(2);
//        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus2.FAILED);
//        assertThat(payments.get(1).getStatus()).isEqualTo(PaymentStatus2.PAID);
//        assertThat(payments.get(1).getOrderNumber()).isEqualTo(expectedV2OrderId);
//
//        // 3. 응답 결과 확인
//        assertThat(result.orderId()).isEqualTo(expectedV2OrderId);
//    }

    @Test
    @DisplayName("기존 결제가 UNCERTAIN 상태라면, 동일한 orderId와 멱등키를 유지하여 재시도한다")
    void uncertain_then_retry_with_same_info() {
        // given
        String originalOrderId = "ORD-UNCERTAIN-400";
        long amount = 50000L;
        String paymentKey = "uncertain_retry_key";
        String existingIdemKey = "existing_idem_key_123";

        // 1. 주문 데이터 삽입 (ID: 800L)
        insertOrder(800L, 1L, originalOrderId, (int) amount, "2026-04-29 13:00:00");

        // 2. 상태가 UNCERTAIN인 Payment2 데이터 삽입 (ID: 980L)
        insertPayment(
                980L,
                800L,
                originalOrderId,
                (int) amount,
                "UNCERTAIN",
                existingIdemKey,
                "PAYMENT"
        );

        // 3. 토스 API Mock 설정 (결국 성공한다고 가정)
        TossConfirmResponse mockResponse = new TossConfirmResponse(paymentKey, originalOrderId, "DONE");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // 승인 요청 DTO
        ConfirmRequest request = new ConfirmRequest(paymentKey, originalOrderId, amount);

        // when
        TossConfirmResponse result = orderConfirmService.sendConfirmRequest(request, null);

        // then
        // 1. 토스로 보낸 요청 캡처 및 검증
        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(TossConfirmResponse.class));
        HttpEntity capturedEntity = entityCaptor.getValue();
        ConfirmRequest capturedBody = (ConfirmRequest) capturedEntity.getBody();
        String capturedIdemKey = capturedEntity.getHeaders().getFirst("Idempotency-Key");

        // [핵심 검증] orderId와 Idempotency-Key가 기존과 동일해야 함
        assertThat(capturedBody.orderId()).isEqualTo(originalOrderId);
        assertThat(capturedIdemKey).isEqualTo(existingIdemKey);

        // 2. DB 검증: 새로운 레코드가 생성되지 않고 기존 레코드가 PAID로 업데이트되어야 함
        List<Payment> payments = paymentRepository.findAllByOrderOrderByCreatedAtAsc(
                orderRepository.findByOrderNumber(originalOrderId).get()
        );

        assertThat(payments).hasSize(1); // v2가 생성되지 않음
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payments.get(0).getId()).isEqualTo(980L); // 기존 그 레코드 그대로

        assertThat(result.status()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("결제가 진행 중(PENDING)인 상태에서 동일한 주문번호로 요청이 오면 예외를 던진다")
    void fail_when_payment_is_pending() {
        // given
        String orderId = "ORD-PENDING-500";
        long amount = 10000L;
        String paymentKey = "new_request_key";

        // 1. 주문 데이터 삽입 (ID: 505L)
        insertOrder(505L, 1L, orderId, (int) amount, "2026-04-29 14:00:00");

        // 2. 현재 진행 중(PENDING)인 결제 데이터를 미리 삽입 (ID: 990L)
        // 아직 만료 시간(5분)이 지나지 않은 상태로 가정
        insertPayment(
                990L,
                505L,
                orderId,
                (int) amount,
                "PENDING",
                "some_idempotency_key",
                "PAYMENT"
        );

        // 승인 요청 DTO
        ConfirmRequest request = new ConfirmRequest(paymentKey, orderId, amount);

        // when & then
        // 3. 결제가 진행 중이므로 BusinessException이 발생해야 함
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, null);
        });
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }
        assertTrue(actualException instanceof BusinessException, "발생한 예외는 HttpServerErrorException.");


        // 4. 예외 코드 확인 (ALREADY_PROCESSED_PAYMENT 등 설정하신 에러코드와 매칭)
        assertThat(((BusinessException) actualException).getErrorCode()).isEqualTo(ALREADY_PROCESSED_PAYMENT);

        // 5. [중요] 토스 API는 절대 호출되지 않아야 함
        verify(restTemplate, times(0)).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("5xx 서버 에러 발생 시 FAILED 상태가 되어야 한다")
    void system_error_transition() {
        // given
        String orderNumber = "ORD-SUCCESS-100";
        ConfirmRequest request = new ConfirmRequest("paymentKey", orderNumber, 10000L);

        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "토스 서버 장애"));


        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, null);
        });
        Throwable finalException = (exception instanceof org.springframework.retry.ExhaustedRetryException)
                ? exception.getCause()
                : exception;

        // then
        assertThat(finalException).isInstanceOf(HttpServerErrorException.class);

        // then
        Payment record = paymentRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}

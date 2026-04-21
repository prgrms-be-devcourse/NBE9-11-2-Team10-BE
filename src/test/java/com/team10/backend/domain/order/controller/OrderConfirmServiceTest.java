package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.order.dto.confirm.ConfirmRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.service.OrderConfirmService;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderConfirmServiceTest {
    private final String TOSS_URL = "https://api.tosspayments.com/v1/payments";
    @Autowired
    private OrderConfirmService orderConfirmService;

    @MockitoBean
    private RestTemplate restTemplate; // 서비스 내부에서 사용하는 RestTemplate을 가로챔

    @ParameterizedTest
    @ValueSource(strings = {"REJECT_ACCOUNT_PAYMENT", "REJECT_CARD_PAYMENT", "REJECT_CARD_COMPANY", "FORBIDDEN_REQUEST", "INVALID_PASSWORD"})
    @DisplayName("403 Forbidden 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_forbiddenGroup2(String errorCode) {
        // 1. Given: RestTemplate이 HttpClientErrorException(403)을 던지도록 설정
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // Toss 에러 응답 바디 시뮬레이션 (ErrorCode.name()이 포함되도록)
        String errorResponseBody = "{\"code\":\"" + errorCode + "\", \"message\":\"테스트 에러\"}";

        HttpClientErrorException forbiddenException = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(),
                StandardCharsets.UTF_8
        );

        // restTemplate 호출 시 에러 발생시킴
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(forbiddenException);

        // 2. When & Then
        // 1. ExhaustedRetryException이 발생함을 기대함
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        // 2. 만약 Retry가 개입했다면 원본 에러(getCause)를 확인
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }

        // 3. 최종 검증
        assertTrue(actualException instanceof BusinessException);
        assertEquals(errorCode, ((BusinessException) actualException).getErrorCode().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_FOUND_PAYMENT", "NOT_FOUND_PAYMENT_SESSION"})
    @DisplayName("404 Not Found 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_notFoundGroup(String errorCode) {
        // 1. Given: RestTemplate이 HttpClientErrorException(404)을 던지도록 설정
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // Toss 에러 응답 바디 시뮬레이션
        String errorResponseBody = "{\"code\":\"" + errorCode + "\", \"message\":\"테스트 에러\"}";

        HttpClientErrorException notFoundException = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(),
                StandardCharsets.UTF_8
        );

        // restTemplate 호출 시 404 에러 발생시킴
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(notFoundException);

        // 2. When & Then
        // Retry 설정이 되어 있을 수 있으므로 상위 Exception으로 잡은 뒤 원본 에러를 확인합니다.
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        // 만약 Retry가 개입했다면 원본 에러(getCause)를 추출
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }

        // 3. 최종 검증: BusinessException 여부와 내부 에러 코드 확인
        assertTrue(actualException instanceof BusinessException);
        assertEquals(errorCode, ((BusinessException) actualException).getErrorCode().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ALREADY_PROCESSED_PAYMENT",
            "INVALID_REQUEST",
            "INVALID_API_KEY",
            "INVALID_REJECT_CARD",
            "INVALID_CARD_EXPIRATION",
            "INVALID_STOPPED_CARD",
            "INVALID_CARD_LOST_OR_STOLEN",
            "INVALID_CARD_NUMBER",
            "INVALID_ACCOUNT_INFO_RE_REGISTER",
            "UNAPPROVED_ORDER_ID"
    })
    @DisplayName("400 Bad Request 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_badRequestGroup(String errorCode) {
        // 1. Given: RestTemplate이 HttpClientErrorException(400)을 던지도록 설정
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // Toss 에러 응답 바디 시뮬레이션 (ErrorCode 포함)
        String errorResponseBody = "{\"code\":\"" + errorCode + "\", \"message\":\"테스트 에러(400)\"}";

        HttpClientErrorException badRequestException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(),
                StandardCharsets.UTF_8
        );

        // restTemplate 호출 시 400 에러 발생 모킹
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(badRequestException);

        // 2. When & Then
        // Retry 발생 가능성을 염두에 두어 최상위 Exception으로 포착
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        // 만약 Retry 정책에 의해 감싸져 있다면 내부 원인(BusinessException)을 확인
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }

        // 3. 최종 검증
        assertTrue(actualException instanceof BusinessException, "발생한 예외는 BusinessException이어야 합니다.");
        assertEquals(errorCode, ((BusinessException) actualException).getErrorCode().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING",
            "UNKNOWN_PAYMENT_ERROR",
            "FAILED_INTERNAL_SYSTEM_PROCESSING"
    })
    @DisplayName("500 Server Error 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_serverErrorGroup(String errorCode) {
        // 1. Given: RestTemplate이 HttpServerErrorException(500)을 던지도록 설정
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // Toss 에러 응답 바디 시뮬레이션
        String errorResponseBody = "{\"code\":\"" + errorCode + "\", \"message\":\"서버 내부 에러(500)\"}";

        // 500 에러를 명시적으로 생성 (HttpServerErrorException)
        HttpServerErrorException serverErrorException = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(),
                StandardCharsets.UTF_8
        );

        // restTemplate 호출 시 500 에러 발생 모킹
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(serverErrorException);

        // 2. When & Then
        // Retry 발생 가능성을 염두에 두어 Exception으로 포착
        Exception exception = assertThrows(Exception.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        // Retry에 의해 감싸져 있다면 원본 BusinessException 추출
        Throwable actualException = exception;
        if (exception instanceof org.springframework.retry.ExhaustedRetryException) {
            actualException = exception.getCause();
        }

        // 3. 최종 검증
        assertTrue(actualException instanceof BusinessException, "발생한 예외는 BusinessException이어야 합니다.");
        assertEquals(errorCode, ((BusinessException) actualException).getErrorCode().name());
    }

    @Test
    @DisplayName("네트워크 에러 발생 시 3번 재시도 후 Recover가 실행되는지 테스트")
    void retry_three_times_and_recover() {
        // given
        ConfirmRequest request = new ConfirmRequest("order_123", "key_abc", 5000L);

        // restTemplate이 호출될 때마다 ResourceAccessException을 던지도록 설정
        // (재시도 횟수인 3번만큼 예외를 발생시킴)
        when(restTemplate.postForEntity(anyString(), any(), eq(TossConfirmResponse.class)))
                .thenThrow(new ResourceAccessException("Network Timeout"));

        // when & then
        // 1. 최종적으로 BusinessException이 발생하는지 확인
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderConfirmService.sendConfirmRequest(request, null);
        });

        // 2. 에러 코드가 네트워크 최종 실패인지 확인
        assertEquals(ErrorCode.NETWORK_ERROR_FINAL_FAILED, exception.getErrorCode());

        // 3. 실제로 restTemplate이 3번 호출되었는지 검증
        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(TossConfirmResponse.class));
    }
}
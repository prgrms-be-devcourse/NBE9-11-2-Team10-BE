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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderConfirmServiceTest {

    @Autowired
    private OrderConfirmService orderConfirmService;

    @MockitoBean
    private RestTemplate restTemplate; // 서비스 내부에서 사용하는 RestTemplate을 가로챔

    @ParameterizedTest
    @ValueSource(strings = {"REJECT_ACCOUNT_PAYMENT", "REJECT_CARD_PAYMENT", "REJECT_CARD_COMPANY", "FORBIDDEN_REQUEST", "INVALID_PASSWORD"})
    @DisplayName("403 Forbidden 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_forbiddenGroup(String errorCode) {
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        assertEquals(errorCode, exception.getErrorCode().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_FOUND_PAYMENT", "NOT_FOUND_PAYMENT_SESSION"})
    @DisplayName("404 Not Found 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_notFoundGroup(String errorCode) {
        // given
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        assertEquals(errorCode, exception.getErrorCode().name());
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
        // given
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        // 발생한 에러 코드가 입력한 에러 코드와 일치하는지 검증
        assertEquals(errorCode, exception.getErrorCode().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING",
            "UNKNOWN_PAYMENT_ERROR",
            "FAILED_INTERNAL_SYSTEM_PROCESSING"
    })
    @DisplayName("500 Server Error 관련 모든 에러 케이스 검증")
    void confirmPayment_fail_serverErrorGroup(String errorCode) {
        // given
        ConfirmRequest request = new ConfirmRequest("test_key", "order_id", 5000L);

        // when & then
        // BusinessException이 발생해야 하며, 에러 코드가 일치해야 함
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderConfirmService.sendConfirmRequest(request, errorCode);
        });

        assertEquals(errorCode, exception.getErrorCode().name());
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
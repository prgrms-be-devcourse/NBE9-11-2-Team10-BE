package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.cancel.CancelRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.team10.backend.global.exception.ErrorCode.*;
import static com.team10.backend.global.exception.ErrorCode.ALREADY_PROCESSED_PAYMENT;
import static com.team10.backend.global.exception.ErrorCode.FORBIDDEN_REQUEST;
import static com.team10.backend.global.exception.ErrorCode.INVALID_REQUEST;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {


    @Value("${custom.toss.payment.secret-key}")
    private String secretKey;

    private final String TOSS_URL = "https://api.tosspayments.com/v1/payments";
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;
    private final IdempotencyService idempotencyService;

    @Retryable(
            value = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TossConfirmResponse sendCancelRequest(String orderId,String paymentKey,
                                                 CancelRequest request) {
        // 1. 헤더 설정 (승인 로직과 동일)
        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

        // 1. 멱등성 레코드 조회 또는 생성 (새 트랜잭션)
        IdempotencyRecord record = idempotencyService.getOrCreateRecord(orderId, RequestType.CANCEL,orderId);

        // 2. 이미 성공한 요청이면 저장된 응답 반환
        if (record.getStatus() == IdempotencyStatus.SUCCESS) {
            return idempotencyService.parseResponse(record.getResponseBody());
        }

        // 3. 작업 시작 처리 (Atomic Update)(동시 요청 방지)
        // 여기서 false가 나오면 '이미 다른 스레드가 PENDING으로 점유 중'이라는 뜻입니다.
        boolean canStart = idempotencyService.startProcessing(record);

        if (!canStart) {
            log.warn("중복된 결제 요청 차단: {}", orderId);
            throw new BusinessException(ALREADY_PROCESSED_PAYMENT); // "현재 결제가 진행 중입니다."
        }
        // 4.  실패(FAILED) 상태일 때만 새로운 키로 갱신
        // 처음 들어온 PENDING 상태라면 이 단계를 건너뛰고 기존 키를 사용합니다.
        if (record.getStatus() == IdempotencyStatus.FAILED) {
            String newTossKey = idempotencyService.generateTossKey(orderId);
            idempotencyService.updateToPendingWithNewKey(record, newTossKey);
        }
        String tossIdempotencyKey = record.getLastTossKey();

        headers.set("Idempotency-Key", tossIdempotencyKey);
        headers.set("Authorization", "Basic " + encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CancelRequest> entity = new HttpEntity<>(request, headers);

        try {
            // 취소 API 엔드포인트: /v1/payments/{paymentKey}/cancel
            String url = TOSS_URL + "/" + paymentKey + "/cancel";
            ResponseEntity<TossConfirmResponse> response = restTemplate.postForEntity(url, entity, TossConfirmResponse.class);

            // 5. 성공 시 결과 저장 및 상태 변경 (SUCCESS)
            idempotencyService.finalizeRecord(record, IdempotencyStatus.SUCCESS, response.getBody());

            log.info("결제 취소 응답 확인: {}, {}", response.getStatusCode(), response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 4xx 에러 처리
            String errorBody = e.getResponseBodyAsString();
            log.error("결제 취소 비즈니스 에러: {}", errorBody);
            idempotencyService.finalizeRecord(record, IdempotencyStatus.FAILED, null);
            handleBusinessError(e.getStatusCode(), errorBody); // 기존 에러 처리기 활용
            throw e;

        } catch (HttpServerErrorException e) {
            // 5xx 에러 처리
            String errorBody = e.getResponseBodyAsString();
            idempotencyService.finalizeRecord(record, IdempotencyStatus.FAILED, null);
            handleSystemError(e.getStatusCode(), errorBody); // 기존 에러 처리기 활용
            throw e;

        } catch (ResourceAccessException e) {
            // 네트워크 에러
            log.error("결제 취소 중 네트워크 통신 실패: {}", e.getMessage());
            throw e;
        }
    }

    @Recover
    public TossConfirmResponse recoverCancel(ResourceAccessException e, String paymentKey, CancelRequest request) {
        log.error("결제 취소 최종 실패 - 모든 재시도 소진. paymentKey: {}, 사유: {}",
                paymentKey, e.getMessage());
        // 취소 실패 시에는 관리자에게 즉시 알림을 보낸다.
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    private void handleBusinessError(HttpStatusCode status, String errorBody) {
        String errorCode = parseErrorCode(errorBody);

        log.error("토스페이먼츠 4xx 에러 발생 - Status: {}, Code: {}", status, errorCode);

        //401
        if (status.equals(HttpStatus.UNAUTHORIZED)) {
            switch (errorCode) {
                case "UNAUTHORIZED_KEY" :
                    throw new BusinessException(UNAUTHORIZED_KEY);

            }
        }

        //404
        if (status.equals(HttpStatus.NOT_FOUND)) {
            switch (errorCode) {
                case "NOT_FOUND_PAYMENT" :
                    throw new BusinessException(NOT_FOUND_PAYMENT );

            }
        }

        // 403Forbidden: 권한이나 상태에 따른 거절
        if (status.equals(HttpStatus.FORBIDDEN)) {
            switch (errorCode) {
                case "NOT_CANCELABLE_PAYMENT" :
                    throw new BusinessException(NOT_CANCELABLE_PAYMENT);
                case "FORBIDDEN_REQUEST":
                    throw new BusinessException(FORBIDDEN_REQUEST);

            }
        }

        // 400 Bad Request:
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            switch (errorCode) {
                case "ALREADY_CANCELED_PAYMENT" :
                    throw new BusinessException(ALREADY_CANCELED_PAYMENT);
                case "INVALID_REQUEST":
                    throw new BusinessException(INVALID_REQUEST);
                case "INVALID_REFUND_ACCOUNT_NUMBER":
                    throw new BusinessException(INVALID_REFUND_ACCOUNT_NUMBER	);
                case "ALREADY_REFUND_PAYMENT":
                    throw new BusinessException(ALREADY_REFUND_PAYMENT);
                case "REFUND_REJECTED":
                    throw new BusinessException(REFUND_REJECTED );
            }
        }

    }

    private void handleSystemError(HttpStatusCode status, String errorBody) {
        // JSON 파싱을 통해 토스의 code와 message 추출
        String errorCode = parseErrorCode(errorBody);

        log.error("토스페이먼츠 5xx 에러 발생 - Status: {}, Code: {}", status, errorCode);
        //todo 관리자나 개발자에게 알람이 가는 로직

        // 3. 토스 서버 및 은행 점검 문제 (500 계열)
        if (status.is5xxServerError()) {
            // 이 경우 트랜잭션을 롤백시켜 DB 주문 삭제를 막아야 함
            switch (errorCode) {
                case "COMMON_ERROR":
                    throw new BusinessException(COMMON_ERROR);
                case "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING":
                    throw new BusinessException(FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING);
                case "FAILED_INTERNAL_SYSTEM_PROCESSING":
                    throw new BusinessException(FAILED_INTERNAL_SYSTEM_PROCESSING );
            }
        }
    }

    private String parseErrorCode(String errorBody) {
        try {
            // 1. String 형태의 JSON을 JsonNode 객체로 읽는다.
            JsonNode root = objectMapper.readTree(errorBody);

            // 2. "code" 필드의 값을 텍스트로 가져온다
            return root.path("code").asText();
        } catch (Exception e) {
            // 파싱 실패 시 로깅 후 기본 에러 코드 반환
            log.error("토스 에러 응답 파싱 중 오류 발생: {}", e.getMessage());
            return "UNKNOWN_ERROR";
        }
    }

}
package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.ConfirmRequest;
import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.domain.order.repository.IdempotencyRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static com.team10.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmService {

    @Value("${custom.toss.payment.secret-key}")
    private String secretKey;

    private final String TOSS_URL = "https://api.tosspayments.com/v1/payments";
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;
    private final IdempotencyService idempotencyService;

    // 1. 재시도 로직
    @Retryable(
            value = { ResourceAccessException.class},
            exclude = { BusinessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TossConfirmResponse sendConfirmRequest(ConfirmRequest request,String testCode) {
        HttpHeaders headers = new HttpHeaders();

        if (testCode != null) {
            headers.add("TossPayments-Test-Code", testCode);
        }

        // 시크릿 키 인증 헤더 설정
        String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        //todo 네트워크가 끊겼을때 다시 시도할 경우 같은 orderNumber 키로 접근하면 오류가 발생한다.

        String orderId = request.orderId();

        // 1. 멱등성 레코드 조회 또는 생성 (새 트랜잭션)
//        IdempotencyRecord record = idempotencyService.getOrCreateRecord(orderId, RequestType.PAYMENT,null);

        //수정
        IdempotencyRecord record2 = idempotencyService.getOrCreateRecord3(orderId, RequestType.PAYMENT,null);

        // 2. 이미 성공한 요청이면 저장된 응답 반환
        if (record2.getStatus() == IdempotencyStatus.SUCCESS) {
            return idempotencyService.parseResponse(record2.getResponseBody());
        }

        // 3. 작업 시작 처리 (Atomic Update)(동시 요청 방지)
        // 여기서 false가 나오면 '이미 다른 스레드가 PENDING으로 점유 중'이라는 뜻입니다.
//        boolean canStart = idempotencyService.startProcessing(record);
//
//        if (!canStart) {
//            log.warn("중복된 결제 요청 차단: {}", orderId);
//            throw new BusinessException(ALREADY_PROCESSED_PAYMENT); // "현재 결제가 진행 중입니다."
//        }
        // 4.  실패(FAILED) 상태일 때만 새로운 키로 갱신
        // 처음 들어온 PENDING 상태라면 이 단계를 건너뛰고 기존 키를 사용합니다.
//        if (record.getStatus() == IdempotencyStatus.FAILED) {
//            String newTossKey = idempotencyService.generateTossKey(orderId);
//            idempotencyService.updateToPendingWithNewKey(record, newTossKey);
//        }
        String tossIdempotencyKey = record2.getLastTossKey();
        headers.set("Idempotency-Key", tossIdempotencyKey+ (testCode != null ? UUID.randomUUID() : ""));
        headers.set("Authorization", "Basic " + encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ConfirmRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TossConfirmResponse> response = restTemplate.postForEntity(TOSS_URL + "/confirm", entity, TossConfirmResponse.class);
            log.info("응답값 확인 {},{}",response,response.getBody());
            // 성공 시 내 DB 업데이트
            idempotencyService.finalizeRecord(record2, IdempotencyStatus.SUCCESS, response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            // 비즈니스 로직 에러 (4xx)
            // 사용자의 잔액 부족, 카드 정보 오류 등
            String errorBody = e.getResponseBodyAsString();
            log.info("에러 바디 확인: {}", errorBody); // 추가
            //null을 전달하여 상태만 FAILED로 변경
            idempotencyService.finalizeRecord(record2, IdempotencyStatus.FAILED, null);
            handleBusinessError(e.getStatusCode(), errorBody);
           throw e; // unreachable (예외가 던져짐)

        } catch (HttpServerErrorException e) {
            // 시스템 및 서버 에러 (5xx)
            // 토스 서버 장애, 은행 점검 등
            String errorBody = e.getResponseBodyAsString();
            log.error("토스 시스템 에러 (5xx): {}",errorBody);
            //토스 서버 문제이므로 FAILED 처리하여 나중에 다시 시도 가능하게 함
            idempotencyService.finalizeRecord(record2, IdempotencyStatus.FAILED, null);
            handleSystemError(e.getStatusCode(), errorBody);
            throw e;

        } catch (ResourceAccessException e) {
            // [네트워크 에러] - 타임아웃, 커넥션 거부 등
            //1. 재시도 로직
            //2. WEBhook을 사용
            //finalizeRecord를 호출하지 않음으로써 DB의 PENDING 상태를 그대로 유지
            log.error("네트워크 통신 실패: {}", e.getMessage());
            idempotencyService.markRecordAsUncertain(record2);
            throw e;
        }
    }


    // 최종적으로 사용자에게 실패 응답을 던지거나,
    @Recover
    public TossConfirmResponse recover(ResourceAccessException e, ConfirmRequest request,String testCode) {
       log.error("결제 승인 최종 실패 - 모든 재시도 소진. 주문번호: {}, 에러: {}",
                request.orderId(), e.getMessage());
//        idempotencyService.finalizeRecord(record, IdempotencyStatus.FAILED, null);
       //todo 관리자에게 알람
        // 네트워크 장애 시: "결제 확인 중" 상태로 변경하거나 관리자 알림
        throw new BusinessException(ErrorCode.NETWORK_ERROR_FINAL_FAILED);
    }

    private void handleBusinessError(HttpStatusCode status, String errorBody) {
        String errorCode = parseErrorCode(errorBody);

        log.error("토스페이먼츠 4xx 에러 발생 - Status: {}, Code: {}", status, errorCode);

        //404
        if (status.equals(HttpStatus.NOT_FOUND)) {
            switch (errorCode) {
                case "NOT_FOUND_PAYMENT" :
                    throw new BusinessException(NOT_FOUND_PAYMENT );
                case "NOT_FOUND_PAYMENT_SESSION":
                    throw new BusinessException(NOT_FOUND_PAYMENT_SESSION);
            }
        }

        // 403Forbidden: 권한이나 상태에 따른 거절
        if (status.equals(HttpStatus.FORBIDDEN)) {
            switch (errorCode) {
                case "REJECT_ACCOUNT_PAYMENT" :
                    throw new BusinessException(REJECT_ACCOUNT_PAYMENT);
                case "REJECT_CARD_PAYMENT":
                    throw new BusinessException(REJECT_CARD_PAYMENT);
                case "REJECT_CARD_COMPANY":
                    throw new BusinessException(REJECT_CARD_COMPANY);
                case "FORBIDDEN_REQUEST":
                    throw new BusinessException(FORBIDDEN_REQUEST);
                case "INVALID_PASSWORD":
                    throw new BusinessException(INVALID_PASSWORD);
            }
        }

        // 400 Bad Request:
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            switch (errorCode) {
                case "ALREADY_PROCESSED_PAYMENT" :
                    throw new BusinessException(ALREADY_PROCESSED_PAYMENT);
                case "INVALID_REQUEST":
                    throw new BusinessException(INVALID_REQUEST);
                case "INVALID_API_KEY":
                    throw new BusinessException(INVALID_API_KEY);
                case "INVALID_REJECT_CARD":
                    throw new BusinessException(INVALID_REJECT_CARD );
                case "INVALID_CARD_EXPIRATION":
                    throw new BusinessException(INVALID_CARD_EXPIRATION );
                case "INVALID_STOPPED_CARD":
                    throw new BusinessException(INVALID_STOPPED_CARD );
                case "INVALID_CARD_LOST_OR_STOLEN":
                    throw new BusinessException(INVALID_CARD_LOST_OR_STOLEN );
                 case "INVALID_CARD_NUMBER":
                    throw new BusinessException(INVALID_CARD_NUMBER );
                 case "INVALID_ACCOUNT_INFO_RE_REGISTER":
                    throw new BusinessException(INVALID_ACCOUNT_INFO_RE_REGISTER);
                 case "UNAPPROVED_ORDER_ID":
                    throw new BusinessException(UNAPPROVED_ORDER_ID);
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
                case "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING":
                    throw new BusinessException(FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING);
                case "UNKNOWN_PAYMENT_ERROR":
                    throw new BusinessException(UNKNOWN_PAYMENT_ERROR);
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

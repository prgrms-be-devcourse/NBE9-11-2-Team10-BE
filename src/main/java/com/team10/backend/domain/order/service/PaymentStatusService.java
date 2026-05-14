package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.Payment;
import com.team10.backend.domain.order.enums.PaymentStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.domain.order.repository.PaymentRepository;
import com.team10.backend.global.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static com.team10.backend.domain.order.enums.PaymentStatus.PAID;
import static com.team10.backend.global.exception.ErrorCode.ALREADY_PROCESSED_PAYMENT;
import static com.team10.backend.global.exception.ErrorCode.PAYMENT_NOT_FOUND;

//import lombok.extern.slf4j.Slf4j;

@Service
//@Slf4j
public class PaymentStatusService {
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    //      네트워크 에러 발생 시 호출:
//     메인 트랜잭션이 롤백되어도 DB에는 'UNCERTAIN' 상태가 남아야 하므로 REQUIRES_NEW 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRecordAsUncertain(Payment record) {
        Payment existing = paymentRepository.findById(record.getId())
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        existing.markAsUncertain(); // 엔티티: this.status = UNCERTAIN
        paymentRepository.saveAndFlush(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment getOrCreatePaymentAttempt(Order order, RequestType type, String originalOrderId) {

        try {
            // 1. 최신 레코드 조회 uncertain의 경우에 필요.이전 결제 상태 값이 필요하다.
            Optional<Payment> latestOpt = paymentRepository.findFirstByOrderOrderByCreatedAtDesc(order);

            if (latestOpt.isPresent()) {
                Payment latestPayment = latestOpt.get();

                // 비관적 락으로 해당 행 점유
                paymentRepository.findByIdForUpdate(latestPayment.getId());

                Payment result = handleExistingPayment(latestPayment);
                if (result != null) return result; //paid, pending, uncertain일때
            }

            // 2. 신규 생성 (FAILED 이후 혹은 최초 생성)
            return createNewPaymentAttempt(order, type);

        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈: 거의 동시에 두 스레드가 신규 생성(v1 등)을 시도했을 경우
//            log.warn("결제 레코드 생성 중 경합 발생 - 최신 데이터 재조회");
            Payment latest = paymentRepository.findFirstByOrderOrderByCreatedAtDesc(order)
                    .orElseThrow(() -> e); // 여전히 없다면 원본 에러 던짐

//            return handleExistingPayment(latest);
            throw new BusinessException(ALREADY_PROCESSED_PAYMENT);
        }
    }

    private Payment createNewPaymentAttempt(Order order, RequestType type) {
        // v1, v2... 접미사 생성을 위한 count
//        int attemptCount = paymentRepository2.countByOrder(order);

        String tossOrderNumber = order.getOrderNumber();
//        String tossOrderNumber = (attemptCount == 0) ? baseOrderNumber : baseOrderNumber + "_v" + (attemptCount);
        String tossIdempotencyKey = generateTossKey(tossOrderNumber);

        Payment newPayment = Payment.createPayment(
                order,
                tossOrderNumber,
                order.getTotalAmount(),
                tossIdempotencyKey,
                type //payment 승인, 혹은 cancel 환불
        );

        return paymentRepository.saveAndFlush(newPayment);
    }

    // 상태 분기 로직 공통화
    private Payment handleExistingPayment(Payment curPayment) {
        switch (curPayment.getStatus()) {
            case PAID:
                return curPayment;
            case UNCERTAIN:
                // 네트워크 에러 등으로 상태가 불분명했던 경우:
                // 토스 가이드에 따라 '동일한 Idempotency-Key'를 유지하며 재시도
                // (이미 성공했을 수도 있으므로 키를 바꾸면 중복 결제 위험이 있음)
                curPayment.markAsPending(); // 다시 PENDING으로 돌리고 진행
                return curPayment;
            case PENDING:
                throw new BusinessException(ALREADY_PROCESSED_PAYMENT);
            case FAILED:
                // null 반환 시 상위 메서드에서 createNewPaymentAttempt() 호출
                return null;//return curPayment;
            default:
                throw new BusinessException(ALREADY_PROCESSED_PAYMENT);
        }
    }
//    private boolean isExpired(IdempotencyRecord record) {
//        // 마지막 업데이트 시간이 5분 전이라면 만료된 요청으로 간주
//        return record.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES));
//    }


    public String generateTossKey(String orderId) {
        return orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // 성공 시 DB에 결과 저장
    @Transactional
    public void finalizeRecord(Payment record, PaymentStatus status, TossConfirmResponse response) {
        Payment payment = paymentRepository.findByIdForUpdate(record.getId())
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        // 2. 이미 PAID인 경우 (웹훅이 먼저 처리한 경우) 바로 리턴
        if (payment.getStatus() == PaymentStatus.PAID) {
//            log.info("이미 완료된 결제입니다. (Race Condition 방어)");
            return;
        }

        if (record.getStatus() == PAID) {
            return;
        }
        if (status == PAID) {
            String jsonResponse = serializeResponse(response);
            record.complete(jsonResponse);
        } else {
            record.failPayment();
        }
        paymentRepository.saveAndFlush(record);

    }

    @Transactional
    public void finalizeRecordFromWebhook(Payment record, WebhookPayload payload) {
        // 클래스명.from(payload) 형태로 호출
        TossConfirmResponse response = TossConfirmResponse.from(payload);
        // 기존 메서드 호출
        this.finalizeRecord(record, PAID, response);
    }

    // JSON -> 객체 변환 (재사용 시)
    public TossConfirmResponse parseResponse(String responseBody) {
        return objectMapper.readValue(responseBody, TossConfirmResponse.class);
    }

    // 객체 -> JSON 변환 (저장 시)
    public String serializeResponse(TossConfirmResponse response) {
        return objectMapper.writeValueAsString(response);
    }

    public PaymentStatusService(PaymentRepository paymentRepository, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }
}

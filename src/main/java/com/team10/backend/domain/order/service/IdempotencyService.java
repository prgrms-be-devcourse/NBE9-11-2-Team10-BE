package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.domain.order.repository.IdempotencyRepository;
import com.team10.backend.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.team10.backend.global.exception.ErrorCode.ALREADY_PROCESSED_PAYMENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private static final int PENDING_TIMEOUT_MINUTES = 5; // 5분 뒤엔 유효하지 않음

//      네트워크 에러 발생 시 호출:
//     메인 트랜잭션이 롤백되어도 DB에는 'UNCERTAIN' 상태가 남아야 하므로 REQUIRES_NEW 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRecordAsUncertain(IdempotencyRecord record) {
        IdempotencyRecord existing = idempotencyRepository.findById(record.getId())
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        existing.markAsUncertain(); // 엔티티: this.status = UNCERTAIN
        idempotencyRepository.saveAndFlush(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord getOrCreateRecord3(String orderId, RequestType type, String originalOrderId) {
        // 1. 먼저 있는지 확인 (락 없이 가볍게)
        Optional<IdempotencyRecord> existingOpt = idempotencyRepository.findByOrderIdAndType(orderId, type);

        if (existingOpt.isPresent()) {
            // 2. 있다면 락을 걸고 다시 조회하여 상태 체크
            IdempotencyRecord existing = idempotencyRepository
                    .findByOrderIdAndTypeForUpdate(orderId, type)
                    .orElseThrow(); // 위에서 확인했으므로 무조건 있음

            return handleExistingRecord(existing, orderId);
        }

        try {
            // 3. 없다면 새로 생성
            String initialKey = generateTossKey(orderId);
            IdempotencyRecord newRecord = (type == RequestType.PAYMENT)
                    ? IdempotencyRecord.createPayment(orderId, initialKey)
                    : IdempotencyRecord.createCancel(orderId, initialKey, originalOrderId);

            return idempotencyRepository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException e) {
            // 그 사이 누군가 인서트했을 아주 희박한 확률을 위한 방어 코드
            return idempotencyRepository.findByOrderIdAndTypeForUpdate(orderId, type)
                    .map(record -> handleExistingRecord(record, orderId))
                    .orElseThrow(() -> e);
        }
    }

    // 상태 분기 로직 공통화
    private IdempotencyRecord handleExistingRecord(IdempotencyRecord existing, String orderId) {
        switch (existing.getStatus()) {
            case SUCCESS: return existing;
            case UNCERTAIN:
                // 네트워크 에러 등으로 상태가 불분명했던 경우:
                // 토스 가이드에 따라 '동일한 Idempotency-Key'를 유지하며 재시도
                // (이미 성공했을 수도 있으므로 키를 바꾸면 중복 결제 위험이 있음)
                existing.markAsPending(); // 다시 PENDING으로 돌리고 진행
                return existing;
            case PENDING:
                if (isExpired(existing)) {
                    log.warn("오래된 PENDING 레코드 발견(롤백 의심). 재시도 허용: {}", orderId);
                    existing.retry(generateTossKey(orderId)); // 새로운 키로 갱신하여 시도 가능케 함
                    return existing;
                }
                throw new BusinessException(ALREADY_PROCESSED_PAYMENT);
            case FAILED:
                String newKey = generateTossKey(orderId);
                existing.retry(newKey);
                return existing;
            default: throw new IllegalStateException("Unexpected status");
        }
    }
    private boolean isExpired(IdempotencyRecord record) {
        // 마지막 업데이트 시간이 5분 전이라면 만료된 요청으로 간주
        return record.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES));
    }
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public IdempotencyRecord getOrCreateRecord(String orderId, RequestType type,String originalOrderId) {
//
//        return idempotencyRepository.findByOrderIdAndType(orderId, type)
//                .orElseGet(() -> {
//                    String initialKey = generateTossKey(orderId);
//                    // 엔티티의 정적 팩토리 메서드 활용
//                    IdempotencyRecord newRecord = (type == RequestType.PAYMENT)
//                            ? IdempotencyRecord.createPayment(orderId, initialKey)
//                            : IdempotencyRecord.createCancel(orderId, initialKey, null); // 취소시 originalOrderId 필요하면 추가
//                    return idempotencyRepository.save(newRecord);
//                });
//    }

    @Transactional
    public boolean startProcessing(IdempotencyRecord record) {
        // 1. 이미 성공한 건 통과
        if (record.getStatus() == IdempotencyStatus.SUCCESS) return true;

        //  현재가 스프링 리트라이에 의한 재시도 상태인지 확인
        int retryCount = RetrySynchronizationManager.getContext() != null
                ? RetrySynchronizationManager.getContext().getRetryCount()
                : 0;

        // 재시도 중(retryCount > 0)이고, 현재 상태가 PENDING이라면 내가 점유한 것이므로 통과
        if (retryCount > 0 && record.getStatus() == IdempotencyStatus.PENDING) {
            return true;
        }

        // 2. 핵심: DB에 직접 물어봄과 동시에 상태를 점유(Update)한다.
        // record 객체의 상태를 묻지 말고 바로 쿼리를 날리낟.
        int updatedRows = idempotencyRepository.updateStatusToPending(record.getOrderId(), record.getType());

        // 3. 업데이트 된 행이 1개라면 선점하는 것에 성공
        if (updatedRows > 0) {
            return true;
        }

        // 4. 선점 실패 시, 혹시 이미 다른 요청이 성공(SUCCESS)시켰는지 DB에서 다시 확인
        // (이미 성공했다면 캐시된 응답을 보내주기 위해 true를 반환해야 함)
        IdempotencyRecord latest = idempotencyRepository.findByOrderIdAndType(record.getOrderId(), record.getType())
                .orElse(record);
        //상태가 여전히 PENDING이라면(다른 스레드가 아직 처리 중), 그때는 false가 반환
        return latest.getStatus() == IdempotencyStatus.SUCCESS;
    }

    @Transactional
    public void updateToPendingWithNewKey(IdempotencyRecord record, String newKey) {
        // 이미 있는 레코드를 업데이트
        record.retry(newKey);
    }

    public String generateTossKey(String orderId) {
        return orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // 성공 시 DB에 결과 저장
    @Transactional
    public void finalizeRecord(IdempotencyRecord record, IdempotencyStatus status, TossConfirmResponse response) {
        try{
           if (record.getStatus() == IdempotencyStatus.SUCCESS) {
                return;
            }
            if (status == IdempotencyStatus.SUCCESS) {
                String jsonResponse = serializeResponse(response);
                record.complete(jsonResponse);
            } else {
                record.fail();
            }
            idempotencyRepository.saveAndFlush(record);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 4. 낙관적 락 충돌 처리
            // 이미 다른 스레드(웹훅 등)가 이 레코드를 업데이트했다는 뜻
            log.info("낙관적 락 충돌: 이미 다른 프로세스가 결제 상태를 업데이트했습니다. OrderId: {}", record.getOrderId());
        }
    }

    @Transactional
    public void finalizeRecordFromWebhook(IdempotencyRecord record, WebhookPayload payload) {
        // 클래스명.from(payload) 형태로 호출
        TossConfirmResponse response = TossConfirmResponse.from(payload);
        // 기존 메서드 호출
        this.finalizeRecord(record, IdempotencyStatus.SUCCESS, response);
    }

    // JSON -> 객체 변환 (재사용 시)
    public TossConfirmResponse parseResponse(String responseBody) {
        return objectMapper.readValue(responseBody, TossConfirmResponse.class);
    }

    // 객체 -> JSON 변환 (저장 시)
    public String serializeResponse(TossConfirmResponse response) {
        return objectMapper.writeValueAsString(response);
    }

}

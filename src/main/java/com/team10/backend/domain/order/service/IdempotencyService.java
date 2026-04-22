package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.domain.order.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord getOrCreateRecord(String orderId, RequestType type,String originalOrderId) {

        return idempotencyRepository.findByOrderIdAndType(orderId, type)
                .orElseGet(() -> {
                    String initialKey = generateTossKey(orderId);
                    // 엔티티의 정적 팩토리 메서드 활용
                    IdempotencyRecord newRecord = (type == RequestType.PAYMENT)
                            ? IdempotencyRecord.createPayment(orderId, initialKey)
                            : IdempotencyRecord.createCancel(orderId, initialKey, null); // 취소시 originalOrderId 필요하면 추가
                    return idempotencyRepository.save(newRecord);
                });
    }

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
        if (status == IdempotencyStatus.SUCCESS) {
            String jsonResponse = serializeResponse(response);
            record.complete(jsonResponse);
        } else {
            record.fail();
        }
        idempotencyRepository.saveAndFlush(record);
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

package com.team10.backend.domain.order.service;

import com.team10.backend.domain.order.dto.confirm.TossConfirmResponse;
import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.entity.IdempotencyRecord;
import com.team10.backend.domain.order.enums.IdempotencyStatus;
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
    public IdempotencyRecord getOrCreateRecord(String orderId) {
        return idempotencyRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    String initialKey = generateTossKey(orderId);
                    return idempotencyRepository.save(new IdempotencyRecord(orderId, initialKey));
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

        // 2. 처음 시도이거나 FAILED 상태일 때만 PENDING으로 변경 시도
        int updatedRows = idempotencyRepository.updateStatusToPending(record.getOrderId());
        return updatedRows > 0;
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

package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.domain.order.enums.RequestType;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        // orderId와 type의 조합이 유니크해야 함 (승인 1건, 취소 1건 저장 가능)
        @UniqueConstraint(columnNames = {"order_id", "type"})
})
public class IdempotencyRecord extends BaseEntity {
    @Column(nullable = false)
    private String orderId;

    @Column(name = "idempotency_key", nullable = false)
    private String lastTossKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type; // 작업 유형: PAYMENT(승인), CANCEL(취소)

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Column(columnDefinition = "TEXT")
    private String responseBody; // 성공 시 저장할 응답 데이터 (JSON)

    // 취소 시 원천 승인 데이터를 찾기 위한 필드 (승인 시에는 null 가능)
    @Column
    private String originalOrderId;

    // 초기 생성 시점 (처음 결제 시도)
    public IdempotencyRecord(String orderId, String initialTossKey, RequestType type, String originalOrderId) {
        this.orderId = orderId;
        this.lastTossKey = initialTossKey;
        this.status = IdempotencyStatus.INIT;
        this.type = type;
        this.originalOrderId = originalOrderId;
    }

    public static IdempotencyRecord createPayment(String orderId, String key) {
        return new IdempotencyRecord(orderId, key, RequestType.PAYMENT, null);
    }

    // 취소 전용 정적 팩토리 메서드 예시
    public static IdempotencyRecord createCancel(String orderId, String key, String originalTxId) {
        return new IdempotencyRecord(orderId, key, RequestType.CANCEL, originalTxId);
    }

    // 재시도 시 토스 키 갱신 및 상태 변경
    public void retry(String newTossKey) {
        this.lastTossKey = newTossKey;
        this.status = IdempotencyStatus.PENDING;
    }

    public void complete(String responseBody) {
        this.status = IdempotencyStatus.SUCCESS;
        this.responseBody = responseBody;
    }

    public void fail() {
        this.status = IdempotencyStatus.FAILED;
    }
}

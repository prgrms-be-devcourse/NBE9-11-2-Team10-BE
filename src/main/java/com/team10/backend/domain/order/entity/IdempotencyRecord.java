package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.IdempotencyStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseEntity {
    @Column(nullable = false,unique = true)
    private String orderId;

    @Column(name = "idempotency_key", nullable = false)
    private String lastTossKey;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Column(columnDefinition = "TEXT")
    private String responseBody; // 성공 시 저장할 응답 데이터 (JSON)

    // 초기 생성 시점 (처음 결제 시도)
    public IdempotencyRecord(String orderId, String initialTossKey) {
        this.orderId = orderId;
        this.lastTossKey = initialTossKey;
        this.status = IdempotencyStatus.INIT;
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

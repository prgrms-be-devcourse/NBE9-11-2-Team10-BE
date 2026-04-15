package com.team10.backend.domain.order.entity;

import com.team10.backend.domain.order.enums.PaymentStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber; // 토스 전송용 고유 번호 (ORD-2026...)

    @Column(name = "total_amount", nullable = false)
    private int totalAmount; // 결제 예정 금액

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // READY, PAID, FAILED

    @Column(name = "payment_key")
    private String paymentKey; // 결제 승인 후 토스에서 받는 키 (초기엔 null)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Builder
    private Payment(Order order, String orderNumber, int totalAmount, PaymentStatus status) {
        this.order = order;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : PaymentStatus.READY;
    }

    // 결제 승인 완료 시 호출할 비즈니스 메서드
    public void completePayment(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.PAID;
    }

    // 결제 실패 시 호출
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}

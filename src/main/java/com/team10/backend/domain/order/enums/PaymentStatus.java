package com.team10.backend.domain.order.enums;

public enum PaymentStatus {
    READY,      // 결제 대기 (가주문 상태)
    PAID,       // 결제 완료
    FAILED,   // 결제 실패

    CANCELLED, // 결제 취소
    REFUNDED; // 환불
}

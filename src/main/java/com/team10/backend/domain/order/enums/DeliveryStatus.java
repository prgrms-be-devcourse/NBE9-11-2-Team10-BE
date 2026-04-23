package com.team10.backend.domain.order.enums;

public enum DeliveryStatus {
    READY,     // 배송 준비 중 (결제 완료 후 생성)
    SHIPPING,  // 배송 중
    COMPLETED  // 배송 완료
}
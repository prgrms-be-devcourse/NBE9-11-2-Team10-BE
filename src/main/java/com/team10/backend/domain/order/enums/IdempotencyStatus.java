package com.team10.backend.domain.order.enums;

public enum IdempotencyStatus {
    INIT,
    PENDING,
    SUCCESS,
    FAILED,
    UNCERTAIN//네트워크 오류 상태
}

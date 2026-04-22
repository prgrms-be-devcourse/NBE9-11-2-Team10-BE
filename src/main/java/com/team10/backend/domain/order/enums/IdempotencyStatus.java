package com.team10.backend.domain.order.enums;

public enum IdempotencyStatus {
    INIT,
    PENDING,
    SUCCESS,
    FAILED
}

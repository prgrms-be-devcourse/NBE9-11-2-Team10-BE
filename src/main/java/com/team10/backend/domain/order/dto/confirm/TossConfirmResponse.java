package com.team10.backend.domain.order.dto.confirm;

public record TossConfirmResponse(
        String paymentKey,
         String orderId,
        String status
) {
}

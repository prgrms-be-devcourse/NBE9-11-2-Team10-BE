package com.team10.backend.domain.order.dto.confirm;

import com.team10.backend.domain.order.dto.webhook.WebhookPayload;

public record TossConfirmResponse(
        String paymentKey,
         String orderId,
        String status
) {
    public static TossConfirmResponse from(WebhookPayload payload) {
        return new TossConfirmResponse(
                payload.data().paymentKey(),
                payload.data().orderId(),
                payload.data().status()
        );
    }
}

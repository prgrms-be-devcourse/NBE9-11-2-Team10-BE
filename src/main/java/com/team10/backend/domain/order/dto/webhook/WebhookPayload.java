package com.team10.backend.domain.order.dto.webhook;

//https://docs.tosspayments.com/reference/using-api/webhook-events#payment_status_changed
public record WebhookPayload(
        String eventType, // PAYMENTS_STATUS_CHANGED 등
        Data data
) {
    public record Data(
            String paymentKey,
            String orderId,
            String status, // DONE, CANCELED, PARTIAL_CANCELED 등
            Long totalAmount
    ) {}
}

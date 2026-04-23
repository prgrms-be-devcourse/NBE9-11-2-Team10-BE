package com.team10.backend.domain.order.controller;

import com.team10.backend.domain.order.dto.webhook.WebhookPayload;
import com.team10.backend.domain.order.service.PaymentWebhookService;
import com.team10.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @PostMapping
    public ApiResponse<Void> handleWebhook(
            @RequestBody WebhookPayload payload
    ) {
        log.info("웹훅 수신: eventType={}, orderId={}", payload.eventType(), payload.data().orderId());

        webhookService.processWebhook(payload);

        return ApiResponse.ok(); // 200 OK를 응답해야 토스가 재전송을 안 함
    }
}
package com.team10.backend.domain.order.dto.confirm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmRequest(
        String paymentKey,
         String orderNumber,
        Long totalAmout
) {}
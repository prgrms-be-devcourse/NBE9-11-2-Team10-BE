package com.team10.backend.domain.order.dto;

public record OrderDeleteResponse(
        String orderNumber,
        String status // "DELETED" 등
) {
}

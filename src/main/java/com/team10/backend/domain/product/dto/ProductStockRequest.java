package com.team10.backend.domain.product.dto;

import jakarta.validation.constraints.Min;

public record ProductStockRequest(
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        int stock
) {
}
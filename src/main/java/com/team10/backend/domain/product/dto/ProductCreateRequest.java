package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.enums.ProductType;

public record ProductCreateRequest(
        String productName,
        int price,
        int stock,
        ProductType type
) {
}
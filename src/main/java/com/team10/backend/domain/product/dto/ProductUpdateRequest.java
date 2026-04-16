package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

public record ProductUpdateRequest(
        String productName,
        String description,
        int price,
        int stock,
        String imageUrl,
        ProductType type,
        ProductStatus status
) {
}

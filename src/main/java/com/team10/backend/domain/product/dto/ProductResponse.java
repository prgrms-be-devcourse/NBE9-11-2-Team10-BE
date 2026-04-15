package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

public record ProductResponse(
        Long productId,
        String productName,
        int price,
        int stock,
        ProductType type,
        ProductStatus status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getPrice(),
                product.getStock(),
                product.getType(),
                product.getStatus()
        );
    }
}
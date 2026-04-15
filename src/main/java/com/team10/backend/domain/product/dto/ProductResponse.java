package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

public record ProductResponse(
        Long productId,
        String productName,
        String description,
        int price,
        int stock,
        String imageUrl,
        ProductType type,
        ProductStatus status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getType(),
                product.getStatus()
        );
    }
}
package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

public record ProductListResponse(
        Long productId,
        String productName,
        int price,
        String imageUrl,
        ProductType type,
        ProductStatus status,
        Long sellerId
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getProductName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getType(),
                product.getStatus(),
                product.getUser().getId()
        );
    }
}
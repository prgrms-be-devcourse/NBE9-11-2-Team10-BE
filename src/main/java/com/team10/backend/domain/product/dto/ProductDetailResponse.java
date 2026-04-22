package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

public record ProductDetailResponse(
        Long productId,
        String productName,
        String description,
        int price,
        int stock,
        String nickname,
        String imageUrl,
        ProductType type,
        ProductStatus status
) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getUser().getNickname(),
                product.getImageUrl(),
                product.getType(),
                product.getStatus()
        );
    }
}
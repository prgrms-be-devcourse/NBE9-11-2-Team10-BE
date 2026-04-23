package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;
import com.team10.backend.domain.product.enums.ProductType;

import java.time.LocalDateTime;

public record ProductListResponse(
        Long productId,
        String productName,
        int price,
        String nickname,
        String imageUrl,
        ProductType type,
        ProductStatus status,
        Long sellerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getProductName(),
                product.getPrice(),
                product.getUser().getNickname(),
                product.getImageUrl(),
                product.getType(),
                product.getStatus(),
                product.getUser().getId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
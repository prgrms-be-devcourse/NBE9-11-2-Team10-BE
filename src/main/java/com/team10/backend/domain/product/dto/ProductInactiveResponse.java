package com.team10.backend.domain.product.dto;

import com.team10.backend.domain.product.entity.Product;
import com.team10.backend.domain.product.enums.ProductStatus;

public record ProductInactiveResponse(
        Long productId,
        ProductStatus status,
        String message
) {
    public static ProductInactiveResponse from(Product product) {
        return new ProductInactiveResponse(
                product.getId(),
                product.getStatus(),
                "상품이 삭제되었습니다."
        );
    }
}
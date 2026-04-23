package com.team10.backend.domain.product.dto;

public record ProductStockResponse(
        Long productId,
        int stock,
        String message
) {
    public static ProductStockResponse of(Long productId, int stock) {
        return new ProductStockResponse(productId, stock, "상품 재고가 수정되었습니다.");
    }
}
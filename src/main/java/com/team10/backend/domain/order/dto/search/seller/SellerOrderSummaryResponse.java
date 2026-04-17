package com.team10.backend.domain.order.dto.search.seller;

import com.team10.backend.domain.order.entity.OrderProducts;

import java.time.LocalDateTime;

public record SellerOrderSummaryResponse(
        String orderNumber,
        String buyerName,        // 구매자 이름
        String productName,      // 내 판매 상품명
        int quantity,            // 판매 수량
        int totalAmount,         // 해당 상품 판매 총액
        LocalDateTime createdAt,
        String status
) {
    public static SellerOrderSummaryResponse from(OrderProducts op) {
        return new SellerOrderSummaryResponse(
                op.getOrder().getOrderNumber(),
                op.getOrder().getUser().getName(),
                op.getProduct().getProductName(),
                op.getQuantity(),
                op.getOrderPrice() * op.getQuantity(),
                op.getOrder().getCreatedAt(),
                op.getOrder().getPayments().get(0).getStatus().name()
        );
    }
}
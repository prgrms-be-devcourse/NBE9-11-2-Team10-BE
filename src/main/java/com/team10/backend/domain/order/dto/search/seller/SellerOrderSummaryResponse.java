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
        // 결제 상태를 안전하게 가져오기 (결제 정보가 없으면 "READY" 또는 "UNKNOWN" 반환)
        String paymentStatus = op.getOrder().getPayments().stream()
                .findFirst() // 첫 번째 결제 정보를 찾음
                .map(payment -> payment.getStatus().name()) // 존재하면 상태명 추출
                .orElse("READY"); // 결제 정보가 전혀 없다면 기본값 반환
        return new SellerOrderSummaryResponse(
                op.getOrder().getOrderNumber(),
                op.getOrder().getUser().getName(),
                op.getProduct().getProductName(),
                op.getQuantity(),
                op.getOrderPrice() * op.getQuantity(),
                op.getOrder().getCreatedAt(),
                paymentStatus
        );
    }
}
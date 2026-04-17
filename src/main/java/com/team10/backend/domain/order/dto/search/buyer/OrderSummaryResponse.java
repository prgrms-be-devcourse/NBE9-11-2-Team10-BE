package com.team10.backend.domain.order.dto.search.buyer;

import com.team10.backend.domain.order.entity.Order;
import com.team10.backend.domain.order.entity.OrderProducts;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        String orderNumber,
        int totalAmount,
        String status,
        String representativeProductName,
        int totalQuantity,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        String productName = order.getOrderProducts().get(0).getProduct().getProductName();
        int extraCount = order.getOrderProducts().size() - 1;
        String representativeName = extraCount > 0 ?
                productName + " 외 " + extraCount + "건" : productName;

        int totalQty = order.getOrderProducts().stream()
                .mapToInt(OrderProducts::getQuantity)
                .sum();

        String paymentStatus = order.getPayments().stream()
                .findFirst()
                .map(payment -> payment.getStatus().name())
                .orElse("READY"); // 결제 전 상태를 기본값으로 설정

        return new OrderSummaryResponse(
                order.getOrderNumber(),
                order.getTotalAmount(),
                paymentStatus,
                representativeName,
                totalQty,
                order.getCreatedAt()
        );
    }
}
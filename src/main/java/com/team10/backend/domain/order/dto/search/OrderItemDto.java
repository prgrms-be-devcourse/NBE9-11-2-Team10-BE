package com.team10.backend.domain.order.dto.search;

import com.team10.backend.domain.order.entity.OrderProducts;

record OrderItemDto(
        Long productId,
        String productName,
        int quantity,
        int orderPrice
) {
    public static OrderItemDto from(OrderProducts op) {
        return new OrderItemDto(
                op.getProduct().getId(),
                op.getProduct().getProductName(),
                op.getQuantity(),
                op.getOrderPrice()
        );
    }
}
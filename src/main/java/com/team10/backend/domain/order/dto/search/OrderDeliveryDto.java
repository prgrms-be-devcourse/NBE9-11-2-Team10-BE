package com.team10.backend.domain.order.dto.search;

import com.team10.backend.domain.order.entity.OrderDelivery;

// 배송 정보를 담는 내부 DTO
record OrderDeliveryDto(
        String deliveryAddress,
        String trackingNumber
) {
    public static OrderDeliveryDto from(OrderDelivery delivery) {
        return new OrderDeliveryDto(
                delivery.getDelivery_address(),
                delivery.getTracking_number()
        );
    }
}
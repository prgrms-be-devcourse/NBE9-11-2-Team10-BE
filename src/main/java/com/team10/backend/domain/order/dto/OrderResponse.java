package com.team10.backend.domain.order.dto;

import com.team10.backend.domain.order.entity.Order;

public record OrderResponse(
        String orderNumber,     // 토스 전송용 orderNumber
        int totalAmount,        // 최종 결제 금액
        Long userId // 구매자 이름
){
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderNumber(),
                order.getTotalAmount(), // Order 엔티티에 해당 필드가 있다고 가정
                order.getUser().getId() // User 객체에서 ID를 추출
        );
    }
}

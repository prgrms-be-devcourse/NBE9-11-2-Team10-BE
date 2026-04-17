package com.team10.backend.domain.order.dto.search;

import com.team10.backend.domain.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

/*
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20240416-ABC123",
    "totalAmount": 150000,
    "paymentStatus": "READY",
    "createdAt": "2024-04-16T14:20:00",
    "delivery": {
      "deliveryAddress": "서울특별시 ...",
      "trackingNumber": "TRK-123456789"
    },
    "orderItems": [
      {
        "productId": 50,
        "productName": "애플 맥북 에어",
        "quantity": 1,
        "orderPrice": 110000
      },
      {
        "productId": 51,
        "productName": "맥북 전용 파우치",
        "quantity": 1,
        "orderPrice": 35000
      },
      {
        "productId": 52,
        "productName": "C타입 허브",
        "quantity": 1,
        "orderPrice": 5000
      }
    ]
  },
  "error": null
}
 */
public record OrderDetailResponse(
        String orderNumber,
        int totalAmount,
        String paymentStatus,
        LocalDateTime createdAt,
        OrderDeliveryDto delivery,      // 배송 정보 (별도 record)
        List<OrderItemDto> orderItems   // 주문 상품 상세 리스트 (별도 record)
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getPayments().isEmpty() ? "UNKNOWN" : order.getPayments().get(0).getStatus().name(),
                order.getCreatedAt(),
                OrderDeliveryDto.from(order.getDelivery()),
                order.getOrderProducts().stream()
                        .map(OrderItemDto::from)
                        .toList()
        );
    }
}
package com.team10.backend.domain.order.dto.search.buyer;

import com.team10.backend.domain.user.entity.User;

import java.util.List;
/*{
  "success": true,
  "data": {
    "userId": 1,
    "userName": "김철수",
    "orders": [
      {
        "orderNumber": "ORD-20240416-a1b2c3d4",
        "totalAmount": 155000,
        "status": "PAID",
        "representativeProductName": "맥북 에어 외 2건",
        "totalQuantity": 3,
        "createdAt": "2024-04-16T10:30:00"
      },
      {
        "orderNumber": "ORD-20240410-z9y8x7w6",
        "totalAmount": 50000,
        "status": "PAID",
        "representativeProductName": "나이키 운동화",
        "totalQuantity": 1,
        "createdAt": "2024-04-10T15:20:00"
      }
    ]
  },
  "error": null
}*/
public record OrderListResponse(
        Long userId,                // 주문자 식별자
        String userName,            // 주문자 이름 (필요 시)
        List<OrderSummaryResponse> orders
) {
    public static OrderListResponse of(User user, List<OrderSummaryResponse> orders) {
        return new OrderListResponse(user.getId(), user.getName(), orders);
    }

}
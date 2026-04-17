package com.team10.backend.domain.order.dto.search.seller;

import com.team10.backend.domain.user.entity.User;

import java.util.List;

/*
{
  "success": true,
  "data": {
    "sellerId": 10,
    "sellerName": "이판매",
    "orders": [
      {
        "orderNumber": "ORD-20240416-ABC123",
        "buyerName": "김구매",
        "productName": "맥북 전용 파우치",
        "quantity": 1,
        "totalAmount": 35000,
        "status": "READY",
        "createdAt": "2024-04-16T14:20:00"
      },
      {
        "orderNumber": "ORD-20240415-DEF456",
        "buyerName": "박철수",
        "productName": "맥북 전용 파우치",
        "quantity": 2,
        "totalAmount": 70000,
        "status": "READY",
        "createdAt": "2024-04-15T09:30:00"
      }
    ]
  },
  "error": null
}
 */
public record SellerOrderListResponse(
        Long sellerId,
        List<SellerOrderSummaryResponse> sales
) {
    public static SellerOrderListResponse of(User seller, List<SellerOrderSummaryResponse> sales) {
        return new SellerOrderListResponse(seller.getId(), sales);
    }
}
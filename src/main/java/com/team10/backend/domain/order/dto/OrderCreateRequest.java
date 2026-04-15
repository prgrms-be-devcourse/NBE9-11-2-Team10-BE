package com.team10.backend.domain.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/*
* {
  "userId": 1,
  "deliveryAddress": "서울특별시 .....",
  "items": [
    { "productId": 101, "quantity": 2 },
    { "productId": 105, "quantity": 1 }
  ]
}*/
public record OrderCreateRequest(

        Long userId,

        @NotBlank(message = "배송 주소는 필수입니다")
        String deliveryAddress,

        @NotEmpty(message = "상품을 최소 1개 이상 선택해야 합니다")
        List<OrderProductReq> orderProducts
) {
    public record OrderProductReq(
            Long productId,
            @Min(1)
            int quantity
    ) {}
}
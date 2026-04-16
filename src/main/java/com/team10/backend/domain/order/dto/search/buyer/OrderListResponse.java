package com.team10.backend.domain.order.dto.search.buyer;

import com.team10.backend.domain.user.entity.User;

import java.util.List;
/**/
public record OrderListResponse(
        Long userId,                // 주문자 식별자
        String userName,            // 주문자 이름 (필요 시)
        List<OrderSummaryResponse> orders
) {
    public static OrderListResponse of(User user, List<OrderSummaryResponse> orders) {
        return new OrderListResponse(user.getId(), user.getName(), orders);
    }

}
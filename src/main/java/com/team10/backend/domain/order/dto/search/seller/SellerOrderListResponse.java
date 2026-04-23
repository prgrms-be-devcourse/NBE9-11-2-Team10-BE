package com.team10.backend.domain.order.dto.search.seller;

import com.team10.backend.domain.user.entity.User;

import java.util.List;


public record SellerOrderListResponse(
        Long sellerId,
        List<SellerOrderSummaryResponse> sales
) {
    public static SellerOrderListResponse of(User seller, List<SellerOrderSummaryResponse> sales) {
        return new SellerOrderListResponse(seller.getId(), sales);
    }
}
package com.team10.backend.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    ACTIVE("판매중", "정상 판매"),
    SOLD_OUT("품절", "재고 없음"),
    INACTIVE("비활성화", "물품 삭제");

    private final String title;
    private final String description;
}

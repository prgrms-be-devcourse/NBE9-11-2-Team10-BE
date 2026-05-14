package com.team10.backend.domain.product.enums;

public enum ProductStatus {

    SELLING("판매중", "정상 판매"),
    SOLD_OUT("품절", "재고 없음"),
    INACTIVE("비활성화", "물품 삭제");

    private final String title;
    private final String description;

    private ProductStatus(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }
}

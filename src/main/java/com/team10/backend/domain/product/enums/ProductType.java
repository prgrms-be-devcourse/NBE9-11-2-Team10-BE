package com.team10.backend.domain.product.enums;

public enum ProductType {

    BOOK("책"),
    EBOOK("전자책");

    private final String title;

    private ProductType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }
}

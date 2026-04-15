package com.team10.backend.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductType {

    BOOK("책"),
    EBOOK("전자책");

    private final String title;
}

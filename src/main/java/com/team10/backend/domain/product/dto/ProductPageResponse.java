package com.team10.backend.domain.product.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
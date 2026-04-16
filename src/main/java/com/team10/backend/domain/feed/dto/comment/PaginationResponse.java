package com.team10.backend.domain.feed.dto.comment;

public record PaginationResponse(
        int currentPage,
        int totalPages,
        long totalElements
) {
}

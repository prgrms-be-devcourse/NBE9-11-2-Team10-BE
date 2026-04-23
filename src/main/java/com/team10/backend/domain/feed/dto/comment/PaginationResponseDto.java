package com.team10.backend.domain.feed.dto.comment;

public record PaginationResponseDto(
        int currentPage,
        int totalPages,
        long totalElements
) {
}

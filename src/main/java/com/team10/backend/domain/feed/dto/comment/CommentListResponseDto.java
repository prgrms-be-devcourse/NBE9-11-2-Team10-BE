package com.team10.backend.domain.feed.dto.comment;

import java.util.List;

public record CommentListResponseDto(
        List<CommentResponseDto> comments,
        PaginationResponseDto pagination
) {
}

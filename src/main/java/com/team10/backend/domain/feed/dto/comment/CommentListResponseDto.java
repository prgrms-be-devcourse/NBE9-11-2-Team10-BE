package com.team10.backend.domain.feed.dto.comment;

import java.util.List;

public record FeedCommentListResponseDto(
        List<FeedCommentResponseDto> comments,
        PaginationResponseDto pagination
) {
}

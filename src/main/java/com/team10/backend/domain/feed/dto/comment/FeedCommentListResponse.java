package com.team10.backend.domain.feed.dto.comment;

import java.util.List;

public record FeedCommentListResponse(
        List<FeedCommentResponse> comments,
        PaginationResponse pagination
) {
}

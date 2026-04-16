package com.team10.backend.domain.feed.dto.comment;

public record FeedCommentLikeResponse(
        boolean isLiked,
        int likeCount
) {
}

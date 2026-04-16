package com.team10.backend.domain.feed.dto.comment;

public record FeedCommentLikeResponseDto(
        boolean isLiked,
        int likeCount
) {
}

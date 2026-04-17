package com.team10.backend.domain.feed.dto.comment;

public record CommentLikeToggleResponseDto(
        boolean isLiked,
        int likeCount
) {
}

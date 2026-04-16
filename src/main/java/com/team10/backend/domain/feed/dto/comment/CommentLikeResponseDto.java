package com.team10.backend.domain.feed.dto.comment;

public record CommentLikeResponseDto(
        boolean isLiked,
        int likeCount
) {
}

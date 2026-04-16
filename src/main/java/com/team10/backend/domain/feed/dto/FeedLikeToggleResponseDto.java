package com.team10.backend.domain.feed.dto;

public record FeedLikeToggleResponseDto(
        boolean isLiked,
        int likeCount
) {

}

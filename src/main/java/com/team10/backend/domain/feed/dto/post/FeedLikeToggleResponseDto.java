package com.team10.backend.domain.feed.dto.post;

public record FeedLikeToggleResponseDto(
        boolean liked,
        int likeCount
) {

}

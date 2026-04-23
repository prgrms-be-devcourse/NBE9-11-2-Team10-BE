package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

public record FeedResponseDto(
        Long feedId,
        String content,
        String imageUrl,
        String createdAt
) {
    public static FeedResponseDto from(FeedPost feedPost) {
        return new FeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl(),
                feedPost.getCreatedAt().toString()
        );
    }
}

package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

public record CreateFeedResponseDto(
        Long feedId,
        String content,
        String imageUrl,
        String createdAt
) {
    public static CreateFeedResponseDto from(FeedPost feedPost) {
        return new CreateFeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl(),
                feedPost.getCreatedAt().toString()
        );
    }
}

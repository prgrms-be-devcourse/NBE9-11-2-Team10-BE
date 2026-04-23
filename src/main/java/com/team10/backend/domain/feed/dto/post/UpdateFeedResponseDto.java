package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

public record UpdateFeedResponseDto(
        Long feedId,
        String content,
        String imageUrl,
        String updatedAt
) {
    public static UpdateFeedResponseDto from(FeedPost feedPost) {
        return new UpdateFeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl(),
                feedPost.getUpdatedAt().toString()
        );
    }
}

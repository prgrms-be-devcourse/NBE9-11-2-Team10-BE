package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

import java.util.List;

public record UpdateFeedResponseDto(
        Long feedId,
        String content,
        List<String> mediaUrls,
        String updatedAt
) {
    public static UpdateFeedResponseDto from(FeedPost feedPost) {
        return new UpdateFeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl() != null && !feedPost.getImageUrl().isBlank()
                        ? List.of(feedPost.getImageUrl())
                        : List.of(),
                feedPost.getUpdatedAt().toString()
        );
    }
}

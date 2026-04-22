package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

import java.util.List;

public record FeedResponseDto(
        Long feedId,
        String content,
        List<String> mediaUrls,
        String createdAt
) {
    public static FeedResponseDto from(FeedPost feedPost) {
        return new FeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl() != null && !feedPost.getImageUrl().isBlank()
                        ? List.of(feedPost.getImageUrl())
                        : List.of(),
                feedPost.getCreatedAt().toString()
        );
    }
}

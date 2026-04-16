package com.team10.backend.domain.feed.dto;

import com.team10.backend.domain.feed.entity.FeedPost;

import java.util.List;

public record CreateFeedResponseDto(
        Long feedId,
        String content,
        List<String> mediaUrls,
        String createdAt
) {
    public static CreateFeedResponseDto from(FeedPost feedPost) {
        return new CreateFeedResponseDto(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl() != null ? List.of(feedPost.getImageUrl()) : List.of(),
                feedPost.getCreatedAt().toString()
        );
    }
}

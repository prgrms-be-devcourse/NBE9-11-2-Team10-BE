package com.team10.backend.domain.feed.dto;

import com.team10.backend.domain.feed.entity.FeedPost;

import java.util.List;

public record CreateFeedResposeDto(
        Long feedId,         // 서버가 생성한 ID
        String content,
        List<String> mediaUrls,
        String createdAt     // 서버가 기록한 시간
) {
    // 엔티티를 받아서 응답 DTO로 변환하는 '정적 팩토리 메서드'
    public static CreateFeedResponse from(FeedPost feedPost) {
        return new CreateFeedResponse(
                feedPost.getId(),
                feedPost.getContent(),
                feedPost.getImageUrl() != null ? List.of(feedPost.getImageUrl()) : List.of(),
                feedPost.getCreatedAt().toString()
        );
    }
}

package com.team10.backend.domain.feed.dto;

import java.util.List;

public record FeedListResponseDto(
        List<FeedDetail> feeds
) {
    public record FeedDetail(
            String feedId,              // UUID
            String content,             // 피드 본문 (최대 2,000자)
            List<String> mediaUrls,     // 이미지/영상 URL (최대 10개)
            int likeCount,              // 좋아요 수
            int commentCount,           // 댓글 수
            boolean isLiked,            // 요청 사용자가 좋아요를 눌렀는지 여부
            String createdAt            // 게시 시간 (ISO 8601)
    ) {}
}


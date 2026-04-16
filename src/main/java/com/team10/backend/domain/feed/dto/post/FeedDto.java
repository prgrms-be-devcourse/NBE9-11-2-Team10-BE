package com.team10.backend.domain.feed.dto.post;

import com.team10.backend.domain.feed.entity.FeedPost;

import java.time.LocalDateTime;

public record FeedDto(
        Long id,
        String imageUrl,
        String content,
        int likeCount,
        int commentCount,
        boolean isLiked,        // 현재 로그인한 유저가 좋아요를 눌렀는지 여부
        boolean isNotice,       // 공지사항 여부
        LocalDateTime createdAt
    ) {
    public static FeedDto from(FeedPost feed, boolean isLiked) {
        return new FeedDto(
                feed.getId(),
                feed.getImageUrl(),
                feed.getContent(),
                feed.getLikeCount(),
                feed.getCommentCount(),
                isLiked,
                false, // 엔티티에 isNotice 필드가 있다면 feed.isNotice()로 변경
                feed.getCreatedAt()
        );
    }
}



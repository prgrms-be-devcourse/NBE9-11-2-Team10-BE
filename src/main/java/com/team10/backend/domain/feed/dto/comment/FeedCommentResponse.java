package com.team10.backend.domain.feed.dto.comment;

import com.team10.backend.domain.feed.entity.FeedComment;
import com.team10.backend.domain.user.entity.User;

public record FeedCommentResponse(
        String commentId,
        Writer writer,
        String content,
        int likeCount,
        boolean isLiked,
        boolean isMine,
        String createdAt,
        String updatedAt
) {
    public static FeedCommentResponse from(FeedComment comment, boolean isLiked, User currentUser) {
        boolean isMine = currentUser != null && comment.getWriter().getId().equals(currentUser.getId());

        return new FeedCommentResponse(
                String.valueOf(comment.getId()),
                Writer.from(comment.getWriter()),
                comment.getContent(),
                comment.getLikeCount(),
                isLiked,
                isMine,
                comment.getCreatedAt().toString(),
                comment.getUpdatedAt().toString()
        );
    }

    public record Writer(
            String userId,
            String nickname,
            String profileImageUrl
    ) {
        public static Writer from(User writer) {
            return new Writer(
                    String.valueOf(writer.getId()),
                    writer.getNickname(),
                    writer.getImageUrl()
            );
        }
    }
}

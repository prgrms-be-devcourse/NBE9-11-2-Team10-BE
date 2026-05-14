package com.team10.backend.domain.feed.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "feed_comment_likes")
public class FeedCommentLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_comment_id", nullable = false)
    private FeedComment feedComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public FeedCommentLike(FeedComment feedComment, User user) {
        this.feedComment = feedComment;
        this.user = user;
    }

    public FeedComment getFeedComment() {
        return this.feedComment;
    }

    public User getUser() {
        return this.user;
    }

    protected FeedCommentLike() {
    }
}

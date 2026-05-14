package com.team10.backend.domain.feed.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feed_comments")
public class FeedComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_post_id", nullable = false)
    private FeedPost feedPost;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @Column(nullable = false)
    private int likeCount = 0;

    @OneToMany(mappedBy = "feedComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedCommentLike> commentLikes = new ArrayList<>();

    public FeedComment(FeedPost feedPost, User writer, String content) {
        this.feedPost = feedPost;
        this.writer = writer;
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updateContent(String content) {
        this.content = content;
    }


    public FeedPost getFeedPost() {
        return this.feedPost;
    }

    public String getContent() {
        return this.content;
    }

    public User getWriter() {
        return this.writer;
    }

    public int getLikeCount() {
        return this.likeCount;
    }

    public List<FeedCommentLike> getCommentLikes() {
        return this.commentLikes;
    }

    protected FeedComment() {
    }
}

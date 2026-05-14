package com.team10.backend.domain.feed.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feed_posts")
public class FeedPost extends BaseEntity {

    @Column
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "feedPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedLike> feedLikes = new ArrayList<>();

    @OneToMany(mappedBy = "feedPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedComment> comments = new ArrayList<>();


    private int likeCount = 0;
    private int commentCount = 0;

    public FeedPost(String imageUrl, String content, User user) {
        this.imageUrl = imageUrl;
        this.content = content;
        this.user = user;
    }

    public void update(String imageUrl, String content) {
        this.imageUrl = imageUrl;
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

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getContent() {
        return this.content;
    }

    public User getUser() {
        return this.user;
    }

    public List<FeedLike> getFeedLikes() {
        return this.feedLikes;
    }

    public List<FeedComment> getComments() {
        return this.comments;
    }

    public int getLikeCount() {
        return this.likeCount;
    }

    public int getCommentCount() {
        return this.commentCount;
    }

    protected FeedPost() {
    }
}

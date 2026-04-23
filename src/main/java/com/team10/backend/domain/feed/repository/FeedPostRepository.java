package com.team10.backend.domain.feed.repository;

import com.team10.backend.domain.feed.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {
    List<FeedPost> findAllByUserIdOrderByCreatedAtDesc(Long sellerId);
}

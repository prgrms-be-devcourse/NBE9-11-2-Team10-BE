package com.team10.backend.domain.feed.repository;

import com.team10.backend.domain.feed.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {
    Optional<FeedLike> findByFeedPostId_AndUser_Id(Long feedPostId, Long userId);
}

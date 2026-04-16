package com.team10.backend.domain.feed.repository;

import com.team10.backend.domain.feed.entity.FeedCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedCommentLikeRepository extends JpaRepository<FeedCommentLike, Long> {

    Optional<FeedCommentLike> findByFeedComment_IdAndUser_Id(Long feedCommentId, Long userId);

    boolean existsByFeedComment_IdAndUser_Id(Long feedCommentId, Long userId);
}

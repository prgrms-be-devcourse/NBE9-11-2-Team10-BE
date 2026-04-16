package com.team10.backend.domain.feed.repository;

import com.team10.backend.domain.feed.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    List<FeedComment> findAllByFeedPostIdOrderByCreatedAtDesc(Long feedPostId);

    Optional<FeedComment> findByIdAndFeedPostId(Long id, Long feedPostId);
}

package com.team10.backend.domain.feed.repository;

import com.team10.backend.domain.feed.entity.FeedComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {
    Page<FeedComment> findAllByFeedPostId(Long feedPostId, Pageable pageable);
    Optional<FeedComment> findByIdAndFeedPostId(Long id, Long feedPostId);
}

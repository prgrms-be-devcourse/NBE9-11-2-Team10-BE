package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.comment.CommentLikeResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentListResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentResponseDto;
import com.team10.backend.domain.feed.dto.comment.CreateFeedCommentRequestDto;
import com.team10.backend.domain.feed.dto.comment.PaginationResponseDto;
import com.team10.backend.domain.feed.entity.FeedComment;
import com.team10.backend.domain.feed.entity.FeedCommentLike;
import com.team10.backend.domain.feed.entity.FeedPost;
import com.team10.backend.domain.feed.repository.FeedCommentLikeRepository;
import com.team10.backend.domain.feed.repository.FeedCommentRepository;
import com.team10.backend.domain.feed.repository.FeedPostRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedCommentService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedCommentLikeRepository feedCommentLikeRepository;

    @Transactional
    public CommentResponseDto createComment(
            Long sellerId,
            Long feedId,
            CreateFeedCommentRequestDto requestDto,
            User currentUser
    ) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        FeedPost feedPost = getFeedPost(sellerId, feedId);
        FeedComment feedComment = feedCommentRepository.save(
                new FeedComment(feedPost, currentUser, requestDto.content())
        );
        feedPost.increaseCommentCount();

        return CommentResponseDto.from(feedComment, false, currentUser);
    }

    public CommentListResponseDto getComments(Long sellerId, Long feedId, User currentUser) {
        getFeedPost(sellerId, feedId);

        List<CommentResponseDto> comments = feedCommentRepository.findAllByFeedPostIdOrderByCreatedAtDesc(feedId)
                .stream()
                .map(comment -> {
                    boolean isLiked = currentUser != null
                            && feedCommentLikeRepository.existsByFeedComment_IdAndUser_Id(
                                    comment.getId(),
                                    currentUser.getId()
                            );
                    return CommentResponseDto.from(comment, isLiked, currentUser);
                })
                .toList();

        return new CommentListResponseDto(
                comments,
                new PaginationResponseDto(0, comments.isEmpty() ? 0 : 1, comments.size())
        );
    }

    @Transactional
    public void deleteComment(Long sellerId, Long feedId, Long commentId, User currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        FeedPost feedPost = getFeedPost(sellerId, feedId);
        FeedComment feedComment = feedCommentRepository.findByIdAndFeedPostId(commentId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_COMMENT_NOT_FOUND));

        if (!feedComment.getWriter().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FEED_COMMENT_ACCESS_DENIED);
        }

        feedCommentRepository.delete(feedComment);
        feedPost.decreaseCommentCount();
    }

    @Transactional
    public CommentLikeResponseDto toggleCommentLike(
            Long sellerId,
            Long feedId,
            Long commentId,
            User currentUser
    ) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        getFeedPost(sellerId, feedId);
        FeedComment feedComment = feedCommentRepository.findByIdAndFeedPostId(commentId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_COMMENT_NOT_FOUND));

        boolean isLiked = feedCommentLikeRepository.findByFeedComment_IdAndUser_Id(commentId, currentUser.getId())
                .map(commentLike -> {
                    feedCommentLikeRepository.delete(commentLike);
                    feedComment.decreaseLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    feedCommentLikeRepository.save(new FeedCommentLike(feedComment, currentUser));
                    feedComment.increaseLikeCount();
                    return true;
                });

        return new CommentLikeResponseDto(isLiked, feedComment.getLikeCount());
    }

    private FeedPost getFeedPost(Long sellerId, Long feedId) {
        FeedPost feedPost = feedPostRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (!feedPost.getUser().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        return feedPost;
    }
}

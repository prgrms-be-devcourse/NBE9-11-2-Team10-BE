package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.comment.CommentLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentListResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentResponseDto;
import com.team10.backend.domain.feed.dto.comment.CreateCommentRequestDto;
import com.team10.backend.domain.feed.dto.comment.PaginationResponseDto;
import com.team10.backend.domain.feed.dto.comment.UpdateCommentRequestDto;
import com.team10.backend.domain.feed.entity.FeedComment;
import com.team10.backend.domain.feed.entity.FeedCommentLike;
import com.team10.backend.domain.feed.entity.FeedPost;
import com.team10.backend.domain.feed.repository.FeedCommentLikeRepository;
import com.team10.backend.domain.feed.repository.FeedCommentRepository;
import com.team10.backend.domain.feed.repository.FeedPostRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedCommentService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedCommentLikeRepository feedCommentLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponseDto createComment(
            Long sellerId,
            Long feedId,
            CreateCommentRequestDto requestDto,
            Long currentUserId
    ) {
        User currentUser = getUser(currentUserId);

        FeedPost feedPost = getFeedPost(sellerId, feedId);
        FeedComment feedComment = feedCommentRepository.save(
                new FeedComment(feedPost, currentUser, requestDto.content())
        );
        feedPost.increaseCommentCount();

        return CommentResponseDto.from(feedComment, false, currentUser);
    }

    public CommentListResponseDto getComments(
            Long sellerId,
            Long feedId,
            int page,
            int size,
            String sort,
            Long currentUserId
    ) {
        getFeedPost(sellerId, feedId);
        User currentUser = getNullableUser(currentUserId);

        Pageable pageable = createPageable(page, size, sort);
        Page<FeedComment> commentPage = feedCommentRepository.findAllByFeedPostId(feedId, pageable);

        var comments = commentPage.getContent().stream()
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
                new PaginationResponseDto(
                        commentPage.getNumber(),
                        commentPage.getTotalPages(),
                        commentPage.getTotalElements()
                )
        );
    }

    private Pageable createPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort safeSort = parseSort(sort);

        return PageRequest.of(safePage, safeSize, safeSort);
    }

    private Sort parseSort(String sort) {
        String[] sortParts = sort == null ? new String[0] : sort.split(",");
        String property = sortParts.length > 0 && "createdAt".equals(sortParts[0])
                ? sortParts[0]
                : "createdAt";
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    @Transactional
    public CommentResponseDto updateComment(
            Long sellerId,
            Long feedId,
            Long commentId,
            UpdateCommentRequestDto requestDto,
            Long currentUserId) {

        User currentUser = getUser(currentUserId);

        getFeedPost(sellerId, feedId);

        FeedComment feedComment = feedCommentRepository.findByIdAndFeedPostId(commentId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!feedComment.getWriter().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        feedComment.updateContent(requestDto.content());


        boolean isLiked = feedCommentLikeRepository.existsByFeedComment_IdAndUser_Id(
                commentId,
                currentUser.getId()
        );

        return CommentResponseDto.from(feedComment, isLiked, currentUser);

    }

    @Transactional
    public void deleteComment(Long sellerId, Long feedId, Long commentId, Long currentUserId) {
        User currentUser = getUser(currentUserId);

        FeedPost feedPost = getFeedPost(sellerId, feedId);

        FeedComment feedComment = feedCommentRepository.findByIdAndFeedPostId(commentId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isCommentWriter = feedComment.getWriter().getId().equals(currentUser.getId());
        boolean isFeedOwner = feedPost.getUser().getId().equals(currentUser.getId());

        if (!isCommentWriter && !isFeedOwner) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        feedCommentRepository.delete(feedComment);
        feedPost.decreaseCommentCount();
    }

    @Transactional
    public CommentLikeToggleResponseDto toggleCommentLike(
            Long sellerId,
            Long feedId,
            Long commentId,
            Long currentUserId
    ) {
        User currentUser = getUser(currentUserId);

        getFeedPost(sellerId, feedId);
        FeedComment feedComment = feedCommentRepository.findByIdAndFeedPostId(commentId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

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

        return new CommentLikeToggleResponseDto(isLiked, feedComment.getLikeCount());
    }

    private FeedPost getFeedPost(Long sellerId, Long feedId) {
        FeedPost feedPost = feedPostRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (!feedPost.getUser().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        return feedPost;
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private User getNullableUser(Long userId) {
        return userId == null ? null : getUser(userId);
    }
}

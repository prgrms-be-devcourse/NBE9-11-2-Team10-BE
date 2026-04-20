package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.post.CreateFeedRequestDto;
import com.team10.backend.domain.feed.dto.post.CreateFeedResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedDto;
import com.team10.backend.domain.feed.dto.post.FeedLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedListResponseDto;
import com.team10.backend.domain.feed.dto.post.UpdateFeedRequestDto;
import com.team10.backend.domain.feed.dto.post.UpdateFeedResponseDto;
import com.team10.backend.domain.feed.entity.FeedLike;
import com.team10.backend.domain.feed.entity.FeedPost;
import com.team10.backend.domain.feed.repository.FeedLikeRepository;
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
public class FeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final FeedLikeRepository feedLikeRepository;

    public FeedListResponseDto getFeedsList(Long sellerId, User currentUser) {
        List<FeedPost> feedPosts = feedPostRepository.findAllByUserIdOrderByCreatedAtDesc(sellerId);

        if (feedPosts.isEmpty()) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        List<FeedDto> feedDtos = feedPosts.stream()
                .map(feed -> {
                    boolean isLiked = false;
                    if (currentUser != null) {
                        isLiked = feed.getFeedLikes().stream()
                                .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));
                    }
                    return FeedDto.from(feed, isLiked);
                })
                .toList();

        return new FeedListResponseDto(feedDtos);
    }

    @Transactional
    public CreateFeedResponseDto createFeed(CreateFeedRequestDto requestDto, User currentUser) {
        FeedPost feedPost = new FeedPost(
                requestDto.mediaUrls() != null && !requestDto.mediaUrls().isEmpty()
                    ? requestDto.mediaUrls().getFirst()
                    : "",
                requestDto.content(),
                currentUser
        );

        FeedPost savedFeed = feedPostRepository.save(feedPost);
        return CreateFeedResponseDto.from(savedFeed);
    }

    @Transactional
    public UpdateFeedResponseDto updateFeed(
            Long sellerId,
            Long feedId,
            UpdateFeedRequestDto requestDto,
            User currentUser
    ) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        FeedPost feedPost = getFeedPost(sellerId, feedId);

        if (!feedPost.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String imageUrl = requestDto.mediaUrls() != null && !requestDto.mediaUrls().isEmpty()
                ? requestDto.mediaUrls().getFirst()
                : "";
        feedPost.update(imageUrl, requestDto.content());
        feedPostRepository.flush();

        return UpdateFeedResponseDto.from(feedPost);
    }

    @Transactional
    public FeedLikeToggleResponseDto toggleFeedLike(Long sellerId, Long feedId, User currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        FeedPost feedPost = getFeedPost(sellerId, feedId);

        boolean isLiked = feedLikeRepository.findByFeedPostId_AndUser_Id(feedId, currentUser.getId())
                .map(feedLike -> {
                    feedLikeRepository.delete(feedLike);
                    feedPost.decreaseLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    feedLikeRepository.save(new FeedLike(feedPost, currentUser));
                    feedPost.increaseLikeCount();
                    return true;
                });

        return new FeedLikeToggleResponseDto(isLiked, feedPost.getLikeCount());
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

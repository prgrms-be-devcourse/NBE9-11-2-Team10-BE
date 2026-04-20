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
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 비로그인 조회도 허용하되, 로그인 유저라면 좋아요 여부를 함께 계산한다.
    public FeedListResponseDto getFeedsList(Long sellerId, Long currentUserId) {
        List<FeedPost> feedPosts = feedPostRepository.findAllByUserIdOrderByCreatedAtDesc(sellerId);

        if (feedPosts.isEmpty()) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        User currentUser = getNullableUser(currentUserId);

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
    // 피드는 판매자만 생성할 수 있다.
    public CreateFeedResponseDto createFeed(CreateFeedRequestDto requestDto, Long currentUserId) {
        User currentUser = getUser(currentUserId);
        validateSeller(currentUser);

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
    // 작성자 본인의 피드만 수정할 수 있다.
    public UpdateFeedResponseDto updateFeed(
            Long feedId,
            UpdateFeedRequestDto requestDto,
            Long currentUserId
    ) {
        FeedPost feedPost = getAuthorizedFeedPost(currentUserId, feedId);

        String imageUrl = requestDto.mediaUrls() != null && !requestDto.mediaUrls().isEmpty()
                ? requestDto.mediaUrls().getFirst()
                : "";

        feedPost.update(imageUrl, requestDto.content());
        feedPostRepository.flush();

        return UpdateFeedResponseDto.from(feedPost);
    }

    @Transactional
    // 좋아요는 로그인 유저라면 누구나 토글할 수 있다.
    public FeedLikeToggleResponseDto toggleFeedLike(Long feedId, Long currentUserId) {
        User currentUser = getUser(currentUserId);

        FeedPost feedPost = getFeedPost(feedId);

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

    @Transactional
    // 작성자 본인의 피드만 삭제할 수 있다.
    public void deleteFeed(Long feedId, Long currentUserId) {
        FeedPost feedPost = getAuthorizedFeedPost(currentUserId, feedId);

        feedPostRepository.delete(feedPost);
        feedPostRepository.flush();
    }

    // 피드를 조회하고 없으면 예외를 던진다.
    private FeedPost getFeedPost(Long feedId) {
        return feedPostRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    // 피드 조회와 작성자 권한 검증을 함께 수행한다.
    private FeedPost getAuthorizedFeedPost(Long userId, Long feedId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        FeedPost feedPost = getFeedPost(feedId);

        if (!feedPost.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return feedPost;
    }

    // 인증 사용자 ID로 도메인 유저를 조회한다.
    private User getUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 선택 로그인 API에서만 사용한다.
    private User getNullableUser(Long userId) {
        return userId == null ? null : getUser(userId);
    }

    // 피드 생성은 판매자 권한이 필요하다.
    private void validateSeller(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getRole() != Role.SELLER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}

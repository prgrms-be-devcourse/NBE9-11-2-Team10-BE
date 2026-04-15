package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.FeedListResponseDto;
import com.team10.backend.domain.feed.entity.FeedPost;
import com.team10.backend.domain.feed.repository.FeedPostRepository;
import com.team10.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedPostService {

    private final FeedPostRepository feedPostRepository;

    public FeedListResponseDto getStoreFeeds(Long sellerId, User currentUser) {
        List<FeedPost> posts = feedPostRepository.findByUser_IdOrderByCreatedAtDesc(sellerId);

        List<FeedListResponseDto.FeedDetail> feedDetails = posts.stream()
                .map(post -> convertToDetail(post, currentUser))
                .toList();

        return new FeedListResponseDto(feedDetails);
    }

    private FeedListResponseDto.FeedDetail convertToDetail(FeedPost post, User currentUser) {
        boolean isLiked = currentUser != null && post.getLikes().stream()
                .anyMatch(user -> user.getId().equals(currentUser.getId()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        return new FeedListResponseDto.FeedDetail(
                String.valueOf(post.getId()),
                post.getContent(),
                post.getImageUrl() != null ? List.of(post.getImageUrl()) : List.of(),
                post.getLikeCount(),
                post.getCommentCount(),
                isLiked,
                post.getCreatedAt().format(formatter)
        );
    }
}


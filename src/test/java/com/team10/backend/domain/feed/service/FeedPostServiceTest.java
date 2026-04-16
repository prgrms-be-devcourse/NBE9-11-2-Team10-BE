package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.post.CreateFeedRequestDto;
import com.team10.backend.domain.feed.dto.post.CreateFeedResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.post.FeedListResponseDto;
import com.team10.backend.domain.feed.repository.FeedLikeRepository;
import com.team10.backend.domain.feed.repository.FeedPostRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class FeedPostServiceTest {

    @Autowired
    private FeedPostRepository feedPostRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private FeedPostService feedPostService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private User buyerUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM feed_likes");
        jdbcTemplate.update("DELETE FROM feed_posts");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                1L,
                "seller@test.com",
                "1234",
                "테스트판매자",
                "seller1",
                "010-1234-5678",
                "서울시",
                "ACTIVE",
                "SELLER"
        );

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                2L,
                "buyer@test.com",
                "1234",
                "테스트구매자",
                "buyer1",
                "010-9999-9999",
                "서울시",
                "ACTIVE",
                "BUYER"
        );

        testUser = userRepository.findById(1L).orElseThrow();
        buyerUser = userRepository.findById(2L).orElseThrow();
    }

    @Test
    @DisplayName("피드 목록 조회 - 성공 (좋아요 여부 포함)")
    void getFeedsList_success() {
        // given
        Long sellerId = 1L;
        jdbcTemplate.update(
                "INSERT INTO feed_posts (image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "https://test.com/image.jpg",
                "테스트 내용",
                sellerId,
                0,
                0
        );

        // when
        FeedListResponseDto result = feedPostService.getFeedsList(sellerId, testUser);

        // then
        assertThat(result.feeds()).hasSize(1);
        assertThat(result.feeds().get(0).content()).isEqualTo("테스트 내용");
    }

    @Test
    @DisplayName("피드 목록 조회 - 데이터 없음 (예외 발생)")
    void getFeedsList_notFound() {

        Long sellerId = 1L;

        assertThatThrownBy(() -> feedPostService.getFeedsList(sellerId, testUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.FEED_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("피드 생성 - 성공")
    void createFeed_success() {

        CreateFeedRequestDto requestDto = new CreateFeedRequestDto(
                "테스트 피드 내용입니다.",
                List.of("https://test-image.com")
        );


        CreateFeedResponseDto result = feedPostService.createFeed(requestDto, testUser);


        assertThat(result.feedId()).isNotNull();
        assertThat(result.content()).isEqualTo("테스트 피드 내용입니다.");
        assertThat(result.mediaUrls().get(0)).isEqualTo("https://test-image.com");
        assertThat(feedPostRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 좋아요 토글 - 좋아요 추가 성공")
    void toggleFeedLike_success() {
        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                100L,
                "https://test.com/image.jpg",
                "좋아요 테스트용 피드",
                1L,
                0,
                0
        );

        FeedLikeToggleResponseDto result = feedPostService.toggleFeedLike(1L, 100L, buyerUser);
        feedLikeRepository.flush();
        feedPostRepository.flush();

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM feed_posts WHERE id = ?",
                Integer.class,
                100L
        );
        Integer feedLikeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_likes WHERE feed_post_id = ? AND user_id = ?",
                Integer.class,
                100L,
                2L
        );

        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(likeCount).isEqualTo(1);
        assertThat(feedLikeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 좋아요 토글 - 좋아요 취소 성공")
    void toggleFeedLike_cancelSuccess() {
        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                101L,
                "https://test.com/image.jpg",
                "좋아요 취소 테스트용 피드",
                1L,
                1,
                0
        );

        jdbcTemplate.update(
                "INSERT INTO feed_likes (feed_post_id, user_id, created_at, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                101L,
                2L
        );

        FeedLikeToggleResponseDto result = feedPostService.toggleFeedLike(1L, 101L, buyerUser);
        feedLikeRepository.flush();
        feedPostRepository.flush();

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM feed_posts WHERE id = ?",
                Integer.class,
                101L
        );
        Integer feedLikeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_likes WHERE feed_post_id = ? AND user_id = ?",
                Integer.class,
                101L,
                2L
        );

        assertThat(result.isLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0);
        assertThat(likeCount).isEqualTo(0);
        assertThat(feedLikeCount).isEqualTo(0);
    }
}

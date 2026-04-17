package com.team10.backend.domain.feed.service;

import com.team10.backend.domain.feed.dto.comment.CommentLikeToggleResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentListResponseDto;
import com.team10.backend.domain.feed.dto.comment.CommentResponseDto;
import com.team10.backend.domain.feed.dto.comment.CreateCommentRequestDto;
import com.team10.backend.domain.feed.dto.comment.UpdateCommentRequestDto;
import com.team10.backend.domain.feed.repository.FeedCommentLikeRepository;
import com.team10.backend.domain.feed.repository.FeedCommentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class FeedCommentServiceTest {

    @Autowired
    private FeedCommentService feedCommentService;

    @Autowired
    private FeedCommentRepository feedCommentRepository;

    @Autowired
    private FeedCommentLikeRepository feedCommentLikeRepository;

    @Autowired
    private FeedPostRepository feedPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM feed_comment_likes");
        jdbcTemplate.update("DELETE FROM feed_comments");
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

        seller = userRepository.findById(1L).orElseThrow();
        buyer = userRepository.findById(2L).orElseThrow();

        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                100L,
                "https://test.com/image.jpg",
                "댓글 테스트용 피드",
                seller.getId(),
                0,
                0
        );
    }

    @Test
    @DisplayName("피드 댓글 생성 - 성공")
    void createComment_success() {
        CreateCommentRequestDto request = new CreateCommentRequestDto("댓글 내용입니다.");

        CommentResponseDto result = feedCommentService.createComment(1L, 100L, request, buyer);
        feedCommentRepository.flush();
        feedPostRepository.flush();

        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM feed_posts WHERE id = ?",
                Integer.class,
                100L
        );

        assertThat(result.commentId()).isNotBlank();
        assertThat(result.content()).isEqualTo("댓글 내용입니다.");
        assertThat(result.writer().userId()).isEqualTo("2");
        assertThat(result.isMine()).isTrue();
        assertThat(feedCommentRepository.count()).isEqualTo(1);
        assertThat(commentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 댓글 조회 - 성공")
    void getComments_success() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                200L,
                100L,
                2L,
                "조회 댓글입니다.",
                1
        );

        jdbcTemplate.update(
                "INSERT INTO feed_comment_likes (feed_comment_id, user_id, created_at, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                200L,
                2L
        );

        CommentListResponseDto result = feedCommentService.getComments(1L, 100L, 0, 20, "createdAt,asc", buyer);

        assertThat(result.comments()).hasSize(1);
        assertThat(result.comments().get(0).content()).isEqualTo("조회 댓글입니다.");
        assertThat(result.comments().get(0).isLiked()).isTrue();
        assertThat(result.comments().get(0).isMine()).isTrue();
        assertThat(result.pagination().currentPage()).isEqualTo(0);
        assertThat(result.pagination().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 댓글 수정 - 성공")
    void updateComment_success() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                205L,
                100L,
                2L,
                "수정 전 댓글입니다.",
                1
        );

        jdbcTemplate.update(
                "INSERT INTO feed_comment_likes (feed_comment_id, user_id, created_at, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                205L,
                2L
        );

        UpdateCommentRequestDto request = new UpdateCommentRequestDto("수정 후 댓글입니다.");

        CommentResponseDto result = feedCommentService.updateComment(1L, 100L, 205L, request, buyer);
        feedCommentRepository.flush();

        String updatedContent = jdbcTemplate.queryForObject(
                "SELECT content FROM feed_comments WHERE id = ?",
                String.class,
                205L
        );
        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM feed_comments WHERE id = ?",
                Integer.class,
                205L
        );

        assertThat(result.commentId()).isEqualTo("205");
        assertThat(result.content()).isEqualTo("수정 후 댓글입니다.");
        assertThat(result.writer().userId()).isEqualTo("2");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isMine()).isTrue();
        assertThat(updatedContent).isEqualTo("수정 후 댓글입니다.");
        assertThat(likeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 댓글 수정 - 작성자가 아니면 예외 발생")
    void updateComment_accessDenied() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                206L,
                100L,
                1L,
                "판매자가 작성한 댓글입니다.",
                0
        );

        UpdateCommentRequestDto request = new UpdateCommentRequestDto("수정 시도입니다.");

        assertThatThrownBy(() -> feedCommentService.updateComment(1L, 100L, 206L, request, buyer))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.COMMENT_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("피드 댓글 삭제 - 성공")
    void deleteComment_success() {
        jdbcTemplate.update(
                "UPDATE feed_posts SET comment_count = ? WHERE id = ?",
                1,
                100L
        );

        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                201L,
                100L,
                2L,
                "삭제 댓글입니다.",
                0
        );

        feedCommentService.deleteComment(1L, 100L, 201L, buyer);
        feedCommentRepository.flush();
        feedPostRepository.flush();

        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM feed_posts WHERE id = ?",
                Integer.class,
                100L
        );

        assertThat(feedCommentRepository.count()).isZero();
        assertThat(commentCount).isEqualTo(0);
    }

    @Test
    @DisplayName("피드 댓글 삭제 - 피드 소유자이면 성공")
    void deleteComment_feedOwnerSuccess() {
        jdbcTemplate.update(
                "UPDATE feed_posts SET comment_count = ? WHERE id = ?",
                1,
                100L
        );

        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                202L,
                100L,
                2L,
                "구매자가 작성한 댓글입니다.",
                0
        );

        feedCommentService.deleteComment(1L, 100L, 202L, seller);
        feedCommentRepository.flush();
        feedPostRepository.flush();

        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM feed_posts WHERE id = ?",
                Integer.class,
                100L
        );

        assertThat(feedCommentRepository.count()).isZero();
        assertThat(commentCount).isEqualTo(0);
    }

    @Test
    @DisplayName("피드 댓글 삭제 - 작성자가 아니면 예외 발생")
    void deleteComment_accessDenied() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                202L,
                100L,
                1L,
                "판매자가 작성한 댓글입니다.",
                0
        );

        assertThatThrownBy(() -> feedCommentService.deleteComment(1L, 100L, 202L, buyer))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.COMMENT_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("피드 댓글 좋아요 토글 - 좋아요 추가 성공")
    void toggleCommentLike_success() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                203L,
                100L,
                1L,
                "좋아요 댓글입니다.",
                0
        );

        CommentLikeToggleResponseDto result = feedCommentService.toggleCommentLike(1L, 100L, 203L, buyer);
        feedCommentLikeRepository.flush();
        feedCommentRepository.flush();

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM feed_comments WHERE id = ?",
                Integer.class,
                203L
        );
        Integer commentLikeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_comment_likes WHERE feed_comment_id = ? AND user_id = ?",
                Integer.class,
                203L,
                2L
        );

        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(likeCount).isEqualTo(1);
        assertThat(commentLikeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 댓글 좋아요 토글 - 좋아요 취소 성공")
    void toggleCommentLike_cancelSuccess() {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                204L,
                100L,
                1L,
                "좋아요 취소 댓글입니다.",
                1
        );

        jdbcTemplate.update(
                "INSERT INTO feed_comment_likes (feed_comment_id, user_id, created_at, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                204L,
                2L
        );

        CommentLikeToggleResponseDto result = feedCommentService.toggleCommentLike(1L, 100L, 204L, buyer);
        feedCommentLikeRepository.flush();
        feedCommentRepository.flush();

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM feed_comments WHERE id = ?",
                Integer.class,
                204L
        );
        Integer commentLikeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_comment_likes WHERE feed_comment_id = ? AND user_id = ?",
                Integer.class,
                204L,
                2L
        );

        assertThat(result.isLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0);
        assertThat(likeCount).isEqualTo(0);
        assertThat(commentLikeCount).isEqualTo(0);
    }
}

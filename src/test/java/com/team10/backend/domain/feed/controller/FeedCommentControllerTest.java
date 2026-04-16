package com.team10.backend.domain.feed.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class FeedCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                100L,
                "https://test.com/image.jpg",
                "댓글 컨트롤러 테스트용 피드",
                1L,
                0,
                0
        );
    }

    @Test
    @DisplayName("피드 댓글 생성 API - 성공")
    void createComment() throws Exception {
        String requestBody = """
                {
                  "content": "댓글 생성 테스트입니다."
                }
                """;

        mockMvc.perform(post("/api/v1/stores/1/feeds/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("댓글 생성 테스트입니다."))
                .andExpect(jsonPath("$.data.writer.userId").value("2"))
                .andExpect(jsonPath("$.data.isMine").value(true));
    }

    @Test
    @DisplayName("피드 댓글 조회 API - 성공")
    void getComments() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                200L,
                100L,
                2L,
                "댓글 조회 테스트입니다.",
                0
        );

        mockMvc.perform(get("/api/v1/stores/1/feeds/100/comments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comments[0].content").value("댓글 조회 테스트입니다."))
                .andExpect(jsonPath("$.data.comments[0].isMine").value(true))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("피드 댓글 삭제 API - 성공")
    void deleteComment() throws Exception {
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
                "댓글 삭제 테스트입니다.",
                0
        );

        mockMvc.perform(delete("/api/v1/stores/1/feeds/100/comments/201")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("피드 댓글 좋아요 API - 성공")
    void toggleCommentLike() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO feed_comments (id, feed_post_id, writer_id, content, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                202L,
                100L,
                1L,
                "댓글 좋아요 테스트입니다.",
                0
        );

        mockMvc.perform(post("/api/v1/stores/1/feeds/100/comments/202/like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }
}

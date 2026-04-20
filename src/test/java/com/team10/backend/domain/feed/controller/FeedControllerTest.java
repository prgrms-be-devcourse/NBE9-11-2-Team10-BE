package com.team10.backend.domain.feed.controller;

import com.team10.backend.domain.feed.repository.FeedPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FeedPostRepository feedPostRepository;

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
                "서초구",
                "ACTIVE",
                "SELLER"
        );

        jdbcTemplate.update(
                "INSERT INTO users " +
                        "(id, email, password, name, nickname, phone_number, address, user_status, role, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                2L,
                "user@test.com",
                "1234",
                "일반유저",
                "user1",
                "010-9876-5432",
                "강남구",
                "ACTIVE",
                "BUYER"
        );
    }

    @Test
    @DisplayName("피드 등록 테스트")
    void createFeed() throws Exception {
        String requestBody = """
            {
              "content": "오늘의 새로운 소식!",
              "mediaUrls": ["https://image.url/1"]
            }
            """;

        mockMvc.perform(post("/api/v1/stores/me/feeds")
                        .principal(new UsernamePasswordAuthenticationToken("1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated()) // 201 확인
                .andExpect(jsonPath("$.data.content").value("오늘의 새로운 소식!"));
    }

    @Test
    @DisplayName("피드 전체 목록 조회 (최신순)")
    void getFeeds() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO feed_posts (image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "https://test.com/image.jpg", // imageUrl
                "조회 테스트용 피드입니다",        // content
                1L,                            // user_id
                0,                             // likeCount
                0                              // commentCount
        );

        jdbcTemplate.update(
                "INSERT INTO feed_posts (image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "https://test.com/image.jpg", // imageUrl
                "조회 테스트용 피드2입니다",        // content
                1L,                            // user_id
                0,                             // likeCount
                0                              // commentCount
        );

        mockMvc.perform(get("/api/v1/stores/1/feeds")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.feeds[0].content").value("조회 테스트용 피드2입니다"))
                .andExpect(jsonPath("$.data.feeds[1].content").value("조회 테스트용 피드입니다"));
    }

    @Test
    @DisplayName("피드 수정 테스트")
    void updateFeed() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                100L,
                "https://test.com/old-image.jpg",
                "수정 전 피드입니다",
                1L,
                0,
                0
        );

        String requestBody = """
            {
              "content": "수정된 피드입니다",
              "mediaUrls": ["https://test.com/new-image.jpg"]
            }
            """;

        mockMvc.perform(patch("/api/v1/stores/me/feeds/100")
                        .principal(new UsernamePasswordAuthenticationToken("1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.feedId").value(100))
                .andExpect(jsonPath("$.data.content").value("수정된 피드입니다"))
                .andExpect(jsonPath("$.data.mediaUrls[0]").value("https://test.com/new-image.jpg"));
    }

    @Test
    @DisplayName("피드 삭제 테스트")
    void deleteFeed() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO feed_posts (id, image_url, content, user_id, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                101L,
                "https://test.com/image.jpg",
                "삭제할 피드입니다",
                1L,
                0,
                0
        );

        mockMvc.perform(delete("/api/v1/stores/me/feeds/101")
                        .principal(new UsernamePasswordAuthenticationToken("1", null))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assert feedPostRepository.findById(101L).isEmpty();
    }

    @Test
    @DisplayName("피드 좋아요 토글 테스트")
    void toggleFeedLike() throws Exception {
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

        mockMvc.perform(post("/api/v1/stores/me/feeds/100/like")
                        .principal(new UsernamePasswordAuthenticationToken("2", null))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }
}

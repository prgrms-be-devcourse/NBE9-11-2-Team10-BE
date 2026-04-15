package com.team10.backend.domain.feed;

import com.team10.backend.domain.feed.entity.FeedPost;
import com.team10.backend.domain.feed.repository.FeedPostRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.enums.UserStatus;
import com.team10.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class FeedPostTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedPostRepository feedPostRepository;

    private User seller;
    private User currentUser;

    @BeforeEach
    void setUp() {
        // 1. 판매자(가게 주인) 생성 및 저장
        seller = new User();
        ReflectionTestUtils.setField(seller, "email", "seller@test.com");
        ReflectionTestUtils.setField(seller, "password", "password123!");
        ReflectionTestUtils.setField(seller, "name", "판매자");
        ReflectionTestUtils.setField(seller, "nickname", "사장님");
        ReflectionTestUtils.setField(seller, "phoneNumber", "010-1111-2222");
        ReflectionTestUtils.setField(seller, "address", "서울");
        ReflectionTestUtils.setField(seller, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(seller, "role", Role.SELLER);
        userRepository.save(seller);

        currentUser = new User();
        ReflectionTestUtils.setField(currentUser, "email", "user@test.com");
        ReflectionTestUtils.setField(currentUser, "password", "password123!");
        ReflectionTestUtils.setField(currentUser, "name", "사용자");
        ReflectionTestUtils.setField(currentUser, "nickname", "손님");
        ReflectionTestUtils.setField(currentUser, "phoneNumber", "010-3333-4444");
        ReflectionTestUtils.setField(currentUser, "address", "부산");
        ReflectionTestUtils.setField(currentUser, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(currentUser, "role", Role.USER);
        userRepository.save(currentUser);

        FeedPost feed1 = new FeedPost("image1.jpg", "첫 번째 피드 내용", seller);
        feedPostRepository.save(feed1);

        FeedPost feed2 = new FeedPost("image2.jpg", "두 번째 피드 내용", seller);
        feedPostRepository.save(feed2);
    }

    @Test
    @DisplayName("피드 전체 조회 성공 - 최신순 정렬 및 데이터 구조 확인")
    void getStoreFeeds_Success() throws Exception {
        // given: sellerId (seller.getId())

        // when & then
        mockMvc.perform(get("/api/v1/stores/{sellerId}/feeds", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeds").isArray())
                .andExpect(jsonPath("$.feeds.length()").value(2))

                //번 인덱스: 가장 최근에 생성된 '두 번째 피드'
                .andExpect(jsonPath("$.feeds.content").value("두 번째 피드 내용"))
                .andExpect(jsonPath("$.feeds.mediaUrls").value("image2.jpg"))
                .andExpect(jsonPath("$.feeds.isLiked").value(false))

                //번 인덱스: 이전에 생성된 '첫 번째 피드'
                .andExpect(jsonPath("$.feeds.content").value("첫 번째 피드 내용"))
                .andExpect(jsonPath("$.feeds.mediaUrls").value("image1.jpg"))

                // 공통 필드 검증
                .andExpect(jsonPath("$.feeds.likeCount").isNumber())
                .andExpect(jsonPath("$.feeds.createdAt").exists());
    }
}
package com.team10.backend.domain.user.integration;

import com.team10.backend.domain.user.controller.UserController;
import com.team10.backend.domain.user.dto.SellerUpdateRequest;
import com.team10.backend.domain.user.dto.UserUpdateRequest;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.domain.user.unit.UserTestFixture;
import com.team10.backend.global.test.AuthTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("유저 프로필 조회 - 성공")
    @WithMockUser(roles = "BUYER")
    void getUserProfile_success() throws Exception {
        User user = UserTestFixture.createBuyer();
        userRepository.save(user);

        AuthTestHelper.setAuth(user);

        ResultActions resultActions = mvc.perform(
                get("/api/v1/users/me"))
                    .andDo(print());

        resultActions
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("getUserProfile"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.name").value("김구매"));
    }

    @Test
    @DisplayName("유저 프로필 수정 - 성공")
    @WithMockUser(roles = "BUYER")
    void updateUserProfile_success() throws Exception {
        User user = UserTestFixture.createBuyer();
        userRepository.save(user);

        AuthTestHelper.setAuth(user);

        UserUpdateRequest request = new UserUpdateRequest(
                "새로운닉네임",
                "010-9999-9999",
                "부산"
        );

        ResultActions resultActions = mvc.perform(
                        put("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("updateMyUserProfile"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.nickname").value("새로운닉네임"));
    }

    @Test
    @DisplayName("판매자 API 접근 실패 - BUYER")
    @WithMockUser(roles = "BUYER")
    void getSellerProfile_fail() throws Exception {
        User user = UserTestFixture.createBuyer();
        userRepository.save(user);

        AuthTestHelper.setAuth(user);

        ResultActions resultActions = mvc.perform(
                get("/api/v1/sellers/me"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("판매자 프로필 조회 - 성공")
    @WithMockUser(roles = "SELLER")
    void getSellerProfile_success() throws Exception {
        User user = UserTestFixture.createSeller();
        userRepository.save(user);

        AuthTestHelper.setAuth(user);

        ResultActions resultActions = mvc.perform(
                get("/api/v1/sellers/me"))
                    .andDo(print());

        resultActions
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("getSellerProfile"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.name").value("송판매"));
    }

    @Test
    @DisplayName("판매자 프로필 수정 - 성공")
    @WithMockUser(roles = "SELLER")
    void updateSellerProfile_success() throws Exception {
        User user = UserTestFixture.createSeller();
        userRepository.save(user);

        AuthTestHelper.setAuth(user);

        SellerUpdateRequest request = new SellerUpdateRequest(
                "새로운판매자",
                "010-9999-9999",
                "부산",
                "새로운 소개입니다",
                "999-99-99999"
        );

        ResultActions resultActions = mvc.perform(
                    put("/api/v1/sellers/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                ).andDo(print());

        resultActions
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(handler().methodName("updateMySellerProfile"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.nickname").value("새로운판매자"))
                .andExpect(jsonPath("$.data.bio").value("새로운 소개입니다"))
                .andExpect(jsonPath("$.data.businessNumber").value("999-99-99999"));
    }
}

package com.team10.backend.domain.auth.integration;

import com.team10.backend.domain.auth.controller.AuthController;
import com.team10.backend.domain.auth.dto.AuthRegisterRequest;
import com.team10.backend.domain.auth.dto.LoginRequest;
import com.team10.backend.domain.user.enums.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("register"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_fail_duplicatedEmail() throws Exception {
        // 먼저 한 번 가입
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        mvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // 같은 이메일로 재가입
        AuthRegisterRequest duplicateRequest = new AuthRegisterRequest(
                "user@example.com",
                "AnotherPass123!",
                "김철수",
                "철수",
                "010-3333-4444",
                "부산광역시 해운대구 123",
                Role.BUYER
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateRequest))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void register_fail_duplicatedNickname() throws Exception {
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        mvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // 같은 닉네임으로 재가입
        AuthRegisterRequest duplicateNicknameRequest = new AuthRegisterRequest(
                "other@example.com",
                "AnotherPass123!",
                "김길동",
                "길동이",
                "010-3333-4444",
                "부산광역시 해운대구 123",
                Role.BUYER
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateNicknameRequest))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("중복 확인 (이메일) - 사용 가능")
    void checkDuplicate_available() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auth/check-duplicate")
                                .param("type", "EMAIL")
                                .param("value", "newuser@example.com")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuthController.class))
                .andExpect(handler().methodName("checkDuplicate"))
                .andExpect(status().isOk());

        resultActions
                .andExpect(jsonPath("$.data.type").value("EMAIL"))
                .andExpect(jsonPath("$.data.value").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("중복 확인 (이메일) - 사용 불가")
    void checkDuplicate_unavailable() throws Exception {
        // 먼저 가입
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        mvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auth/check-duplicate")
                                .param("type", "EMAIL")
                                .param("value", "user@example.com")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("중복 확인 실패 - type 파라미터 누락")
    void checkDuplicate_fail1() throws Exception {
        mvc.perform(
                    get("/api/v1/auth/check-duplicate")
                            .param("value", "test@example.com")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 확인 실패 - value 파라미터 누락")
    void checkDuplicate_fail2() throws Exception {
        mvc.perform(
                        get("/api/v1/auth/check-duplicate")
                                .param("type", "EMAIL")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 확인 실패 - 존재하지 않는 type 값")
    void checkDuplicate_fail3() throws Exception {
        mvc.perform(
                        get("/api/v1/auth/check-duplicate")
                                .param("type", "PHONE")
                                .param("value", "010-1111-2222")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        AuthRegisterRequest registerRequest = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울",
                Role.BUYER
        );

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // when
        LoginRequest loginRequest = new LoginRequest(
                "user@example.com",
                "SecurePass123!"
        );

        ResultActions result = mvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andDo(print());

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 오류")
    void login_fail_wrong_password() throws Exception {
        // given
        AuthRegisterRequest registerRequest = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울",
                Role.BUYER
        );

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // when
        LoginRequest loginRequest = new LoginRequest(
                "user@example.com",
                "wrong-password"
        );

        ResultActions result = mvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andDo(print());

        // then
        result.andExpect(status().isBadRequest());
        result.andExpect(cookie().doesNotExist("accessToken"));
        result.andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        AuthRegisterRequest registerRequest = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울",
                Role.BUYER
        );

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // login
        LoginRequest loginRequest = new LoginRequest(
                "user@example.com",
                "SecurePass123!"
        );

        ResultActions loginResult = mvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        );

        Cookie refreshCookie = loginResult.andReturn()
                .getResponse()
                .getCookie("refreshToken");

        assert refreshCookie != null;

        // when
        ResultActions result = mvc.perform(
                post("/api/v1/auth/logout")
                        .cookie(refreshCookie)
        ).andDo(print());

        // then
        result.andExpect(status().isOk());

        result.andExpect(cookie().maxAge("accessToken", 0));
        result.andExpect(cookie().maxAge("refreshToken", 0));
    }

}

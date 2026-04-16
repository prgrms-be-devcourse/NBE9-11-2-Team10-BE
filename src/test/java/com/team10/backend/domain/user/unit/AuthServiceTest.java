package com.team10.backend.domain.user.unit;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.dto.DuplicateCheckResponse;
import com.team10.backend.domain.user.dto.LoginRequest;
import com.team10.backend.domain.user.dto.LoginResult;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.repository.AuthRepository;
import com.team10.backend.domain.user.service.AuthService;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import com.team10.backend.global.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // given
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        given(authRepository.existsByEmail(request.email())).willReturn(false);
        given(authRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        User user = User.create(request, "encodedPassword");
        given(authRepository.save(any(User.class))).willReturn(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // when
        AuthRegisterResponse response = authService.register(request);

        // then
        then(authRepository).should(times(1)).existsByEmail(request.email());
        then(authRepository).should(times(1)).existsByNickname(request.nickname());
        then(passwordEncoder).should(times(1)).encode(request.password());
        then(authRepository).should(times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(request.email(), savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());

        assertEquals(request.email(), response.email());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_fail_duplicate_email() {
        // given
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        given(authRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));

        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());

        then(authRepository).should(times(1)).existsByEmail(request.email());
        then(authRepository).should(never()).save(any());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void register_fail_duplicate_nickname() {
        // given
        AuthRegisterRequest request = new AuthRegisterRequest(
                "user@example.com",
                "SecurePass123!",
                "홍길동",
                "길동이",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.BUYER
        );

        given(authRepository.existsByEmail(request.email())).willReturn(false);
        given(authRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));

        assertEquals(ErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());

        then(authRepository).should(never()).save(any());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    @DisplayName("중복 확인 - 이메일 사용 가능")
    void checkDuplicate_email_available() {
        // given
        given(authRepository.existsByEmail("user@example.com")).willReturn(false);

        // when
        DuplicateCheckResponse response = authService.checkDuplicate(DuplicateType.EMAIL, "user@example.com");

        // then
        assertEquals(DuplicateType.EMAIL, response.type());
        assertEquals("user@example.com", response.value());
        assertTrue(response.available());
    }

    @Test
    @DisplayName("중복 확인 - 이메일 사용 불가")
    void checkDuplicate_email_unavailable() {
        // given
        given(authRepository.existsByEmail("user@example.com")).willReturn(true);

        // when
        DuplicateCheckResponse response = authService.checkDuplicate(DuplicateType.EMAIL, "user@example.com");

        // then
        assertFalse(response.available());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "password");

        User user = User.builder()
                        .email(request.email())
                        .password("encodedPassword")
                        .nickname("길동이")
                        .role(Role.BUYER)
                        .build();

        given(authRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword()))
                            .willReturn(true);
        given(tokenProvider.generateToken(user.getId(), user.getRole())).willReturn("test-access-token");

        // when
        LoginResult result = authService.login(request);

        // then
        assertEquals(user.getEmail(), result.response().email());
        assertEquals("test-access-token", result.accessToken());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_mismatch_password() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "password");

        User user = User.builder()
                .email(request.email())
                .password("encodedPassword")
                .nickname("길동이")
                .role(Role.BUYER)
                .build();

        given(authRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());

        then(tokenProvider).should(never()).generateToken(any(), any());
    }


}

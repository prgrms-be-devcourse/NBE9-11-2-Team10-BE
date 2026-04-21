package com.team10.backend.domain.auth.unit;

import com.team10.backend.domain.auth.dto.AuthRegisterRequest;
import com.team10.backend.domain.auth.dto.AuthRegisterResponse;
import com.team10.backend.domain.auth.dto.DuplicateCheckResponse;
import com.team10.backend.domain.auth.dto.LoginRequest;
import com.team10.backend.domain.auth.dto.LoginResult;
import com.team10.backend.domain.auth.service.AuthService;
import com.team10.backend.domain.auth.service.RefreshTokenService;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - BUYER")
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

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        User user = User.create(request, "encodedPassword", request.role());
        given(userRepository.save(any(User.class))).willReturn(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // when
        AuthRegisterResponse response = authService.register(request);

        // then
        then(userRepository).should(times(1)).existsByEmail(request.email());
        then(userRepository).should(times(1)).existsByNickname(request.nickname());
        then(passwordEncoder).should(times(1)).encode(request.password());
        then(userRepository).should(times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(request.email(), savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.BUYER, savedUser.getRole());
        assertNull(savedUser.getSellerInfo());

        assertEquals(request.email(), response.email());
    }

    @Test
    @DisplayName("회원가입 성공 - SELLER")
    void register_success_seller() {
        // given
        AuthRegisterRequest request = new AuthRegisterRequest(
                "seller@example.com",
                "SecurePass123!",
                "홍길동",
                "셀러",
                "010-1111-2222",
                "서울특별시 강남구 테헤란로 123",
                Role.SELLER
        );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        User user = User.create(request, "encodedPassword", request.role());
        given(userRepository.save(any(User.class))).willReturn(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // when
        AuthRegisterResponse response = authService.register(request);

        // then
        then(userRepository).should(times(1)).existsByEmail(request.email());
        then(userRepository).should(times(1)).existsByNickname(request.nickname());
        then(passwordEncoder).should(times(1)).encode(request.password());
        then(userRepository).should(times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(Role.SELLER, savedUser.getRole());
        assertNotNull(savedUser.getSellerInfo());
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

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));

        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());

        then(userRepository).should(times(1)).existsByEmail(request.email());
        then(userRepository).should(never()).save(any());
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

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));

        assertEquals(ErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());

        then(userRepository).should(never()).save(any());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    @DisplayName("중복 확인 - 이메일 사용 가능")
    void checkDuplicate_email_available() {
        // given
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);

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
        given(userRepository.existsByEmail("user@example.com")).willReturn(true);

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

        given(this.userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword()))
                            .willReturn(true);
        given(tokenProvider.generateToken(user.getId(), user.getRole())).willReturn("test-access-token");
        given(refreshTokenService.createRefreshToken(user)).willReturn("test-refresh-token");

        // when
        LoginResult result = authService.login(request);

        // then
        assertEquals(user.getEmail(), result.response().email());
        assertEquals("test-access-token", result.accessToken());
        assertEquals("test-refresh-token", result.refreshToken());
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

        given(this.userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());

        then(tokenProvider).should(never()).generateToken(any(), any());
        then(refreshTokenService).should(never()).createRefreshToken(any());
    }

}

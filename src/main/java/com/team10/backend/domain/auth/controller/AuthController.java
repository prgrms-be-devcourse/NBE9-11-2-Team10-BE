package com.team10.backend.domain.auth.controller;

import com.team10.backend.domain.auth.dto.*;
import com.team10.backend.domain.auth.service.AuthService;
import com.team10.backend.domain.auth.service.RefreshTokenService;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.team10.backend.global.constant.CookieConstants.ACCESS_TOKEN;
import static com.team10.backend.global.constant.CookieConstants.REFRESH_TOKEN;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Auth", description = "회원가입 및 인증 API")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "사용자 회원가입을 진행합니다.")
    public ApiResponse<AuthRegisterResponse> register(@RequestBody @Valid AuthRegisterRequest request) {
        AuthRegisterResponse response = authService.register(request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/check-duplicate")
    @Operation(summary = "중복 확인",
            description = "type(email, nickname)에 따라 값의 중복 여부를 확인합니다.")
    public ApiResponse<DuplicateCheckResponse> checkDuplicate(
            @RequestParam @NotNull DuplicateType type,
            @RequestParam @NotBlank String value
    ) {
        DuplicateCheckResponse response = authService.checkDuplicate(type, value);
        return ApiResponse.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인을 진행합니다.")
    public ApiResponse<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(request);
        cookieUtil.addCookie(response, ACCESS_TOKEN, result.accessToken());
        cookieUtil.addCookie(response, REFRESH_TOKEN, result.refreshToken());

        return ApiResponse.ok(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "401에러 시 토큰을 재발급 받습니다.")
    public ApiResponse<Void> refresh(HttpServletRequest request,
                                     HttpServletResponse response
    ) {
        String refreshToken = cookieUtil.getCookieValue(request, REFRESH_TOKEN);
        RefreshResult result = refreshTokenService.refresh(refreshToken);

        cookieUtil.addCookie(response, ACCESS_TOKEN, result.accessToken());
        cookieUtil.addCookie(response, REFRESH_TOKEN, result.refreshToken());

        return ApiResponse.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 진행합니다.")
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    HttpServletResponse response
    ) {
        String refreshToken = cookieUtil.getCookieValue(request, REFRESH_TOKEN);
        refreshTokenService.revoke(refreshToken);

        cookieUtil.deleteCookie(response, ACCESS_TOKEN);
        cookieUtil.deleteCookie(response, REFRESH_TOKEN);

        return ApiResponse.ok();
    }

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.cookieUtil = cookieUtil;
    }
}

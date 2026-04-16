package com.team10.backend.domain.user.controller;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.dto.DuplicateCheckResponse;
import com.team10.backend.domain.user.dto.LoginRequest;
import com.team10.backend.domain.user.dto.LoginResponse;
import com.team10.backend.domain.user.dto.LoginResult;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.service.AuthService;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "회원가입 및 인증 API")
public class AuthController {

    private final AuthService authService;
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
        cookieUtil.addCookie(response, "accessToken", result.accessToken());

        return ApiResponse.ok(result.response());
    }

}

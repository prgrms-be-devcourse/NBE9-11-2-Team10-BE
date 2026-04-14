package com.team10.backend.domain.user.controller;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.dto.DuplicateCheckResponse;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.service.AuthService;
import com.team10.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthRegisterResponse> register(@RequestBody AuthRegisterRequest request) {
        AuthRegisterResponse response = authService.register(request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/check-duplicate")
    public ApiResponse<DuplicateCheckResponse> checkEmail(
                                        @RequestParam DuplicateType type,
                                        @RequestParam String value) {
        DuplicateCheckResponse response = authService.checkDuplicate(type, value);
        return ApiResponse.ok(response);
    }


}

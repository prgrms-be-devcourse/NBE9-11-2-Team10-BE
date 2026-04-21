package com.team10.backend.domain.user.controller;

import com.team10.backend.domain.user.dto.SellerPublicResponse;
import com.team10.backend.domain.user.dto.SellerResponse;
import com.team10.backend.domain.user.dto.SellerUpdateRequest;
import com.team10.backend.domain.user.dto.UserResponse;
import com.team10.backend.domain.user.dto.UserUpdateRequest;
import com.team10.backend.domain.user.service.UserService;
import com.team10.backend.global.dto.ApiResponse;
import com.team10.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name =  "User/Seller Profile", description = "사용자 및 판매자 프로필 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<UserResponse> getUserProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserResponse response = userService.getUserProfile(principal.userId());

        return ApiResponse.ok(response);
    }

    @GetMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerResponse> getSellerProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        SellerResponse response = userService.getSellerProfile(principal.userId());

        return ApiResponse.ok(response);
    }

    @GetMapping("/sellers/{id}")
    public ApiResponse<SellerPublicResponse> getSellerPublicProfile(
            @PathVariable Long id
    ) {
        SellerPublicResponse response = userService.getSellerPublicProfile(id);

        return ApiResponse.ok(response);
    }

    @PutMapping("/users/me")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<UserResponse> updateMyUserProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse response = userService.updateMyUserProfile(principal.userId(), request);

        return ApiResponse.ok(response);
    }

    @PutMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<SellerResponse> updateMySellerProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody SellerUpdateRequest request
    ) {
        SellerResponse response = userService.updateMySellerProfile(principal.userId(), request);

        return ApiResponse.ok(response);
    }


}

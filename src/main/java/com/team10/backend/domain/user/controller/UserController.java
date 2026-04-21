package com.team10.backend.domain.user.controller;

import com.team10.backend.domain.user.dto.SellerResponse;
import com.team10.backend.domain.user.dto.SellerUpdateRequest;
import com.team10.backend.domain.user.dto.UserResponse;
import com.team10.backend.domain.user.dto.UserUpdateRequest;
import com.team10.backend.domain.user.service.UserService;
import com.team10.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<UserResponse> getUserProfile() {
        UserResponse response = userService.getUser();

        return null;
    }

    @GetMapping("/sellers/me")
    public ApiResponse<SellerResponse> getSellerProfile() {
        return null;
    }

    @GetMapping("/sellers/{id}")
    public ApiResponse<SellerResponse> getSellerInfo() {
        return null;
    }

    @PutMapping("/users/me")
    public ApiResponse<UserResponse> updateMyUserProfile(
            @RequestBody UserUpdateRequest request
    ) {
        return null;
    }

    @PutMapping("sellers/me")
    public ApiResponse<SellerResponse> updateMySellerProfile(
            @RequestBody SellerUpdateRequest request
    ) {
        return null;
    }


}

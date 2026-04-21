package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.entity.User;

public record UserResponse(
    Long id,
    String imageUrl,
    String email,
    String name,
    String nickname,
    String phoneNumber,
    String address
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getImageUrl(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.getAddress()
        );
    }

}

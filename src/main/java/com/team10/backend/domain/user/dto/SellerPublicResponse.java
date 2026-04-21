package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.entity.User;

public record SellerPublicResponse(
        String imageUrl,
        String name,
        String nickname,
        String bio
) {

    public static SellerPublicResponse from(User user) {
        return new SellerPublicResponse(
                user.getImageUrl(),
                user.getName(),
                user.getNickname(),
                user.getSellerInfo().getBio()
        );
    }
}

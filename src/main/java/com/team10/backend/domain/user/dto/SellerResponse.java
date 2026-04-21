package com.team10.backend.domain.user.dto;

import com.team10.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record SellerResponse(
    Long id,
    String imageUrl,
    String email,
    String name,
    String nickname,
    String phoneNumber,
    String bio,
    String businessNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static SellerResponse from(User user) {
        return new SellerResponse(
                user.getId(),
                user.getImageUrl(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.getSellerInfo().getBio(),
                user.getSellerInfo().getBusinessNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}

package com.team10.backend.domain.user.dto;

public record SellerPublicResponse(
        String imageUrl,
        String name,
        String nickname,
        String bio
) {}

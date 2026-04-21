package com.team10.backend.domain.user.dto;

public record SellerResponse(
    Long id,
    String imageUrl,
    String email,
    String name,
    String nickname,
    String phoneNumber,
    String bio,
    String createdAt,
    String updatedAt
) {}

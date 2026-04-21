package com.team10.backend.domain.user.dto;

public record UserResponse(
    Long id,
    String imageUrl,
    String email,
    String name,
    String nickname,
    String phoneNumber,
    String address
) {}

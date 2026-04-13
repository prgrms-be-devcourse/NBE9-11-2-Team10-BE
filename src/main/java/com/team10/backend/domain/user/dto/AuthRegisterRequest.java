package com.team10.backend.domain.user.dto;

public record AuthRegisterRequest(
        String imageUrl,
        String email,
        String password,
        String name,
        String nickname,
        String phoneNumber,
        String address
) {}

package com.team10.backend.domain.user.dto;

public record UserUpdateRequest(
    String nickname,
    String phoneNumber,
    String address
) {}

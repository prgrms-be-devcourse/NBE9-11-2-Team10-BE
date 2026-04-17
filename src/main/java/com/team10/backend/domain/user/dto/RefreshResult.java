package com.team10.backend.domain.user.dto;

public record RefreshResult(
        String accessToken,
        String refreshToken
) {}

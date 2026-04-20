package com.team10.backend.domain.auth.dto;

public record RefreshResult(
        String accessToken,
        String refreshToken
) {}

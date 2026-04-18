package com.team10.backend.domain.auth.service;

import com.team10.backend.domain.auth.entity.RefreshToken;
import com.team10.backend.domain.auth.repository.RefreshTokenRepository;
import com.team10.backend.domain.user.dto.RefreshResult;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import com.team10.backend.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    @Transactional
    public String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.create(token, user);
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public RefreshResult refresh(String token) {
        RefreshToken oldRefreshToken = validateRefreshToken(token);
        oldRefreshToken.revoke();

        User user = oldRefreshToken.getUser();

        String newAccessToken = tokenProvider.generateToken(user.getId(), user.getRole());
        String newRefreshToken = createRefreshToken(user);

        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    private RefreshToken validateRefreshToken(String token) {
        if(token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 로그아웃 상태 || 이미 사용한 토큰 || 만료일이 지난 토큰
        if(refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(token).ifPresent(RefreshToken::revoke);
    }

}

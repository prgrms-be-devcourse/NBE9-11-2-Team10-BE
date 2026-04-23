package com.team10.backend.domain.auth.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String token;

    private LocalDateTime expiresAt;

    private boolean isRevoked;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    public static RefreshToken create(String token, User user) {
        return RefreshToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .user(user)
                .build();
    }

    public void update(String token) {
        this.token = token;
        this.expiresAt = LocalDateTime.now().plusDays(30);
    }

    public void revoke() {
        this.isRevoked = true;
    }
}

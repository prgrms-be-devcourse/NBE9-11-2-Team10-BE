package com.team10.backend.domain.auth.entity;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
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

    public String getToken() {
        return this.token;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public boolean isRevoked() {
        return this.isRevoked;
    }

    public User getUser() {
        return this.user;
    }

    public RefreshToken(String token, LocalDateTime expiresAt, boolean isRevoked, User user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.isRevoked = isRevoked;
        this.user = user;
    }

    public RefreshToken() {
    }

    public static class RefreshTokenBuilder {
        private String token;
        private LocalDateTime expiresAt;
        private boolean isRevoked;
        private User user;

        RefreshTokenBuilder() {
        }

        public RefreshTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public RefreshTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public RefreshTokenBuilder isRevoked(boolean isRevoked) {
            this.isRevoked = isRevoked;
            return this;
        }

        public RefreshTokenBuilder user(User user) {
            this.user = user;
            return this;
        }

        public RefreshToken build() {
            return new RefreshToken(this.token, this.expiresAt, this.isRevoked, this.user);
        }

        public String toString() {
            return "RefreshToken.RefreshTokenBuilder(token=" + this.token + ", expiresAt=" + this.expiresAt + ", isRevoked=" + this.isRevoked + ", user=" + this.user + ")";
        }
    }

    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }
}

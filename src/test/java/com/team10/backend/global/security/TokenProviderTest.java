package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TokenProviderTest {

    private TokenProvider tokenProvider;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        String secretKey = "RjnNJn0bpT1nvG1AgH2vbTOvkB1iMzlX3+Evusj2n/U=";
        long expireTime = 1;

        tokenProvider = new TokenProvider(secretKey, expireTime);

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    @DisplayName("토큰 생성 테스트")
    void generateToken_success() {
        // given
        Long userId = 1L;
        Role role = Role.ROLE_BUYER;

        // when
        String token = tokenProvider.generateToken(userId, role);

        // then
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("토큰에 id와 role이 포함된다")
    void token_contains_claims() {
        // given
        String token = tokenProvider.generateToken(1L, Role.ROLE_BUYER);

        // when
        Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        // then
        assertEquals("1", claims.getSubject());
        assertEquals("BUYER", claims.get("role").toString());
    }

}

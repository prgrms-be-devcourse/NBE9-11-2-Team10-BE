package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Role role = Role.BUYER;

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
        String token = tokenProvider.generateToken(1L, Role.BUYER);

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

    @Test
    @DisplayName("토큰 파싱이 정상적으로 된다")
    void parseClaims_success() {
        // given
        String token = tokenProvider.generateToken(1L, Role.BUYER);

        // when
        Claims claims = tokenProvider.parseClaims(token);

        // then
        assertEquals("1", claims.getSubject());
        assertEquals("BUYER", claims.get("role"));
    }

    @Test
    @DisplayName("Authentication 객체가 정상 생성되고 권한 정보가 제대로 들어간다")
    void getAuthentication_success() {
        // given
        String token = tokenProvider.generateToken(1L, Role.BUYER);

        // when
        Authentication authentication = tokenProvider.getAuthentication(token);

        // then
        assertNotNull(authentication);
        assertEquals("1", authentication.getName());
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(
                authentication.getAuthorities().stream()
                        .anyMatch(a ->
                                Objects.equals(a.getAuthority(), "ROLE_BUYER")));
    }

//    @Test
//    @DisplayName("시간이 만료된 토큰은 예외가 발생한다")
//    void parseClaims_fail_expireToken() throws InterruptedException {
//        String token = tokenProvider.generateToken(1L, Role.BUYER);
//
//        Thread.sleep(60001);
//
//        // when & then
//        assertThrows(ExpiredJwtException.class, () -> {
//            tokenProvider.parseClaims(token);
//        });
//    }

    @Test
    @DisplayName("잘못된 토큰은 예외가 발생한다")
    void parseClaims_fail() {
        // given
        String invalidToken = "fake.token.value";

        // when & then
        assertThrows(Exception.class, () -> {
            tokenProvider.parseClaims(invalidToken);
        });
    }

}

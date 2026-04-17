package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

public class TokenProvider {

    private final long expireTime;
    private final SecretKey key;

    public TokenProvider(String secretKey, long expireTime) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expireTime = expireTime;
    }

    public String generateToken(Long id, Role role) {
        Date issuedAt = new Date();
        Date expireDate = calculateExpireDate(issuedAt);

        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    private Date calculateExpireDate(Date issuedAt) {
        return new Date(issuedAt.getTime() + expireTime * 1000 * 60);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        UserDetails userDetails = new User(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}

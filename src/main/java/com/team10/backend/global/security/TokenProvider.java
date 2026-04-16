package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenProvider {

    @Value("${custom.jwt.expireTime}")
    private long expireTime;

    private final SecretKey key;

    public TokenProvider(@Value("${custom.jwt.secretKey}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long id, Role role) {
        Date issuedAt = new Date();
        Date expireDate = new Date(issuedAt.getTime() + expireTime * 1000 * 60);

        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }


}

package com.team10.backend.global.security;

import com.team10.backend.domain.user.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

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


}

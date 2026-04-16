package com.team10.backend.global.config;

import com.team10.backend.global.security.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${custom.jwt.secretKey}")
    private String secretKey;

    @Value("${custom.jwt.expireTime}")
    private long expireTime;

    @Bean
    public TokenProvider tokenProvider() {
        return new TokenProvider(secretKey, expireTime);
    }

}

package com.team10.backend.global.security;

import com.team10.backend.global.exception.ErrorResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .headers((headers) -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json; charset=UTF-8");
                            response.setStatus(401);
                            ProblemDetail problem = ErrorResponseUtil.buildProblemDetail(
                                    HttpStatus.UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    "인증 정보가 없습니다.",
                                    request
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(problem));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json; charset=UTF-8");
                            response.setStatus(403);
                            ProblemDetail problem = ErrorResponseUtil.buildProblemDetail(
                                    HttpStatus.FORBIDDEN,
                                    "FORBIDDEN",
                                    "접근 권한이 없습니다.",
                                    request
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(problem));
                        })
                );

        return http.build();
    }
}

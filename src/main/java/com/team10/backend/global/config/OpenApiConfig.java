package com.team10.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // ProblemDetail 확장 필드 정의
        Schema<?> errorSchema = new Schema<>()
                .type("object")
                .description("RFC 7807 Problem Detail + 확장 필드")
                .addProperty("type", new StringSchema().example("https://api.example.com/errors/USER_001"))
                .addProperty("title", new StringSchema().example("Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("회원을 찾을 수 없습니다."))
                .addProperty("instance", new StringSchema().example("/api/v1/users/999"))
                .addProperty("errorCode", new StringSchema().example("USER_001"))
                .addProperty("timestamp", new StringSchema().example("2026-04-14T10:30:00"))
                .addProperty("traceId", new StringSchema().example("a1b2c3d4"));

        return new OpenAPI()
                .components(new Components().addSchemas("ProblemDetail", errorSchema));
    }
}
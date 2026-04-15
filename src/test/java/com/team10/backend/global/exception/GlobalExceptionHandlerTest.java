package com.team10.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest  // ✅ 전체 컨텍스트 로드 (안정성 최우선)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("필수 필드 5개가 모두 정확히 응답된다")
    void requiredFields_areReturned() throws Exception {
        String expectedCode = ErrorCode.USER_NOT_FOUND.getCode();
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.example.com/errors/" + expectedCode))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("회원을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errorCode").value(expectedCode))
                .andExpect(jsonPath("$.instance").value("/test/business"));
    }

    @Test
    @DisplayName("traceId 가 응답에 포함되면 UUID 포맷을 따른다")
    void traceId_ifPresent_hasValidFormat() throws Exception {
        mockMvc.perform(get("/test/business")
                        .requestAttr("traceId", "a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.traceId").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    }

    @Test
    @DisplayName("검증 실패 시 필수 필드 + validationErrors 확장 필드가 포함된다")
    void validationFailure_returnsProblemDetailWithErrors() throws Exception {
        String invalidJson = "{\"name\":\"\",\"email\":\"not-an-email\"}";

        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.example.com/errors/VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.instance").value("/test/valid"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[?(@.field=='email')]").exists());
    }

    @Test
    @DisplayName("내부 오류 시 민감 정보가 detail 에 노출되지 않는다")
    void unexpectedException_doesNotExposeSensitiveInfo() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.example.com/errors/INTERNAL_ERROR"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.instance").value("/test/unexpected"))
                .andExpect(jsonPath("$.detail", not(containsString("테스트용 예외"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
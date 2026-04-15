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

    @Test
    @DisplayName("타입 변환 실패 (MethodArgumentTypeMismatchException)")
    void handleTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/param-type")
                        .param("id", "not-a-number")) // Integer 파라미터에 문자열 전달
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("타입 변환 실패")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    @DisplayName("필수 파라미터 누락 (MissingServletRequestParameterException)")
    void handleMissingParam() throws Exception {
        mockMvc.perform(get("/test/param-missing")) // name 파라미터 없이 요청
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("PARAM_MISSING"))
                .andExpect(jsonPath("$.detail").value("필수 파라미터 누락: 'name'"));
    }

    @Test
    @DisplayName("파라미터 @NotBlank 검증 실패")
    void handleMethodValidationFailure() throws Exception {
        mockMvc.perform(get("/test/validated")
                        .param("code", "  "))  // 공백만 전달 → @NotBlank 실패
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                // ✅ 수정된 기대값: "code: must not be blank" 포함 확인
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("code")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("must not be blank")));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 (405)")
    void handleMethodNotAllowed() throws Exception {
        // POST 엔드포인트를 GET 으로 호출
        mockMvc.perform(get("/test/method-test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("지원하지 않는 HTTP 메서드")));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type (415)")
    void handleUnsupportedMediaType() throws Exception {
        // JSON 이 필요한 엔드포인트에 text/plain 전송
        mockMvc.perform(post("/test/media-type-test")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.detail").value("지원하지 않는 Content-Type 입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 경로 (404 - NoHandlerFoundException)")
    void handleNotFound() throws Exception {
        mockMvc.perform(get("/test/this-path-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("찾을 수 없습니다")));
    }

    @Test
    @DisplayName("DB 무결성 위반 (409 - DataIntegrityViolationException)")
    void handleDataIntegrityViolation() throws Exception {
        // TestController 에서 직접 예외 던지기로 시뮬레이션
        mockMvc.perform(get("/test/db-violation"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.detail").value("이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다."));
    }
}
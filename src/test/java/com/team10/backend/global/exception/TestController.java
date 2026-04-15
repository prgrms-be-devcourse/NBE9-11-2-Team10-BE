package com.team10.backend.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Component
@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/business")
    public void business() { throw new BusinessException(ErrorCode.USER_NOT_FOUND); }

    @GetMapping("/unexpected")
    public void unexpected() { throw new RuntimeException("테스트용 예외 - 노출되면 안 됨"); }

    @PostMapping(value = "/valid", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void valid(@Valid @RequestBody TestDto dto) {}  // ✅ Spring @RequestBody 사용

    @GetMapping("/param-type")
    public void typeMismatch(@RequestParam Integer id) {}

    @GetMapping("/param-missing")
    public void missingParam(@RequestParam String name) {}

    @GetMapping("/validated")
    public void constraintViolation(@RequestParam @NotBlank String code) {}

    @GetMapping("/not-found-test")
    public void notFoundTest() {}  // 존재하지 않는 경로는 별도로 테스트

    @PostMapping("/method-test")
    public void methodTest() {}  // GET 으로 호출 시 405 테스트용

    @PostMapping(value = "/media-type-test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void mediaTypeTest(@RequestBody Map<String, Object> body) {}  // wrong Content-Type 테스트용

    @GetMapping("/db-violation")
    public void dbViolation() {
        // 실제 DB 에러 시뮬레이션
        throw new DataIntegrityViolationException("Duplicate entry for key 'email'");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Component
    static class TestDto {
        @NotBlank
        private String name;
        @Email
        private String email;
    }
}
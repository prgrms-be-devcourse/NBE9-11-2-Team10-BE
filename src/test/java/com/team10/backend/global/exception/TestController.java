package com.team10.backend.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

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
package com.team10.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {  // ✅ extends ResponseEntityExceptionHandler 제거!

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        problemDetail.setTitle(e.getStatus().getReasonPhrase());
        problemDetail.setType(URI.create("https://api.example.com/errors/" + e.getErrorCode().getCode()));
        problemDetail.setProperty("errorCode", e.getErrorCode().getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("instance", request.getRequestURI());

        String traceId = (String) request.getAttribute("traceId");
        if (traceId != null) {
            problemDetail.setProperty("traceId", traceId);
        }
        return ResponseEntity.status(e.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "입력값 검증에 실패했습니다. (" + details + ")"
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.example.com/errors/VALIDATION_FAILED"));
        problemDetail.setProperty("errorCode", "VALIDATION_FAILED");
        problemDetail.setProperty("instance", request.getRequestURI());
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> java.util.Map.of(
                        "field", err.getField(),
                        "message", java.util.Optional.ofNullable(err.getDefaultMessage()).orElse("입력값이 올바르지 않습니다")
                ))
                .collect(Collectors.toList());
        problemDetail.setProperty("validationErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleJsonParseException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "요청 본문 형식이 올바르지 않습니다."
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.example.com/errors/INVALID_JSON"));
        problemDetail.setProperty("errorCode", "INVALID_JSON");
        problemDetail.setProperty("instance", request.getRequestURI());
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.example.com/errors/INTERNAL_ERROR"));
        problemDetail.setProperty("errorCode", "INTERNAL_ERROR");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("instance", request.getRequestURI());

        String traceId = (String) request.getAttribute("traceId");
        if (traceId != null) {
            problemDetail.setProperty("traceId", traceId);
        }
        return ResponseEntity.internalServerError().body(problemDetail);
    }
}
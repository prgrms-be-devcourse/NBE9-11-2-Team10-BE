package com.team10.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String detail = "타입 변환 실패: 매개변수 '" + e.getName() + "'의 값 '" + e.getValue() + "' 이(가) 올바르지 않습니다.";
        return buildBadRequestProblem("TYPE_MISMATCH", detail, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String detail = "필수 파라미터 누락: '" + e.getParameterName() + "'";
        return buildBadRequestProblem("PARAM_MISSING", detail, request);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException e, HttpServletRequest request) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return buildBadRequestProblem("CONSTRAINT_VIOLATION", detail, request);
    }

    // 공통 응답 생성 유틸 (중복 코드 제거용)
    private ResponseEntity<ProblemDetail> buildBadRequestProblem(
            String errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://api.example.com/errors/" + errorCode));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("instance", request.getRequestURI());
        problem.setProperty("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "요청한 리소스를 찾을 수 없습니다. (" + e.getRequestURL() + ")", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "지원하지 않는 HTTP 메서드입니다. 허용: " + e.getSupportedHttpMethods(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type 입니다.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        // 실제 DB 오류 메시지는 노출하지 않고 일반화
        return buildProblemDetail(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE",
                "이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다.", request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(
            HandlerMethodValidationException e, HttpServletRequest request) {

        List<String> errorMessages = e.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String paramName = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream()
                            .map(error -> {
                                String message = error.getDefaultMessage();
                                return String.format("%s: %s",
                                        paramName != null ? paramName : "unknown",
                                        message != null ? message : "유효성 검사 오류");
                            });
                })
                .collect(Collectors.toList());

        String detail = errorMessages.isEmpty()
                ? "입력값 검증에 실패했습니다."
                : "입력값 검증에 실패했습니다. (" + String.join(", ", errorMessages) + ")";

        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                detail,
                request);
    }

    // 🔹 중복 코드 제거용 공통 응답 생성 메서드
    private ResponseEntity<ProblemDetail> buildProblemDetail(
            HttpStatus status, String errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://api.example.com/errors/" + errorCode));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("instance", request.getRequestURI());
        problem.setProperty("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(problem);
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

    @ExceptionHandler(org.springframework.retry.ExhaustedRetryException.class)
    public ResponseEntity<ProblemDetail> handleRetryExhaustedException(org.springframework.retry.ExhaustedRetryException e, HttpServletRequest request) {
        log.error("=== ExhaustedRetryException 발생 분석 ===");
        log.error("- 1단계 Cause: {}", e.getCause() != null ? e.getCause().getClass().getName() : "null");

        if (e.getCause() != null && e.getCause().getCause() != null) {
            log.error("- 2단계 Cause: {}", e.getCause().getCause().getClass().getName());
        }

        // 원인 예외를 끝까지 추적해서 BusinessException을 찾는 로직
        Throwable rootCause = e;
        while (rootCause != null) {
            if (rootCause instanceof BusinessException businessException) {
                // 팀원이 작성한 handleBusinessException 로직과 100% 동일하게 구현
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                        businessException.getStatus(),
                        businessException.getMessage()
                );
                problemDetail.setTitle(businessException.getStatus().getReasonPhrase());

                // 팀 표준: 에러 타입 URI 추가
                problemDetail.setType(URI.create("https://api.example.com/errors/" + businessException.getErrorCode().getCode()));

                problemDetail.setProperty("errorCode", businessException.getErrorCode().getCode());
                problemDetail.setProperty("timestamp", LocalDateTime.now());
                problemDetail.setProperty("instance", request.getRequestURI());

                // 팀 표준: traceId 처리 추가
                String traceId = (String) request.getAttribute("traceId");
                if (traceId != null) {
                    problemDetail.setProperty("traceId", traceId);
                }

                return ResponseEntity.status(businessException.getStatus()).body(problemDetail);
            }
            rootCause = rootCause.getCause();
        }

        log.error("비즈니스 예외를 찾지 못해 일반 에러 처리를 진행합니다.");
        return handleUnexpectedException(e, request);
    }
}

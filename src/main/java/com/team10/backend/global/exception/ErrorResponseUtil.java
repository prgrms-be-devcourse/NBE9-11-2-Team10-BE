package com.team10.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.LocalDateTime;

public class ErrorResponseUtil {

    public static ProblemDetail buildProblemDetail(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://api.example.com/errors/" + errorCode));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("instance", request.getRequestURI());
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

}

package com.example.engine.api;

import com.example.engine.domain.GameConflictException;
import com.example.engine.domain.GameNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail handleNotFound(GameNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Game not found", exception.getMessage(), request);
    }

    @ExceptionHandler(GameConflictException.class)
    public ProblemDetail handleConflict(GameConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.title(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .findFirst()
            .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request body", "Malformed JSON or unsupported field value", request);
    }

    private String formatFieldError(FieldError error) {
        return "%s %s".formatted(error.getField(), error.getDefaultMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}

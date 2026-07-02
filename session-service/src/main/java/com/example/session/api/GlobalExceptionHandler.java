package com.example.session.api;

import com.example.session.domain.SessionConflictException;
import com.example.session.domain.SessionNotFoundException;
import com.example.session.engine.EngineUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail handleNotFound(SessionNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Session not found", exception.getMessage(), request);
    }

    @ExceptionHandler(SessionConflictException.class)
    public ProblemDetail handleConflict(SessionConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Session conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(EngineUnavailableException.class)
    public ProblemDetail handleEngineUnavailable(EngineUnavailableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, "Engine unavailable", exception.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}

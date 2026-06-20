package com.lahat.muolana.config;

import com.lahat.muolana.shared.exceptions.ConflictException;
import com.lahat.muolana.shared.exceptions.DomainException;
import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import com.lahat.muolana.shared.exceptions.UnprocessableEntityException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE = "https://api.muolana.ss/errors/";

    private final Environment environment;

    GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.warn("Validation error: {}", exception.getMessage());
        List<Map<String, String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    assert fieldError.getDefaultMessage() != null;
                    return Map.of("field", fieldError.getField(), "message", fieldError.getDefaultMessage());
                })
                .toList();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, "Request body contains invalid fields");
        problemDetail.setType(URI.create(ERROR_BASE + "validation-failed"));
        problemDetail.setTitle("Validation failed");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handle(DomainException exception, WebRequest request) {
        log.warn("Domain error: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setType(URI.create(ERROR_BASE + "bad-request"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handle(ResourceNotFoundException exception, WebRequest request) {
        log.warn("Not found: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
        problemDetail.setType(URI.create(ERROR_BASE + "not-found"));
        problemDetail.setTitle("Not Found");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handle(ConflictException exception, WebRequest request) {
        log.warn("Conflict: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
        problemDetail.setType(URI.create(ERROR_BASE + "conflict"));
        problemDetail.setTitle("Conflict");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    ProblemDetail handle(UnprocessableEntityException exception, WebRequest request) {
        log.warn("Unprocessable: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(UNPROCESSABLE_ENTITY, exception.getMessage());
        problemDetail.setType(URI.create(ERROR_BASE + "unprocessable"));
        problemDetail.setTitle("Unprocessable Entity");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unexpected error", exception);
        String detail = isDevelopmentMode() ? exception.getMessage() : "An unexpected error occurred";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, detail);
        problemDetail.setType(URI.create(ERROR_BASE + "internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(extractPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private boolean isDevelopmentMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("local");
    }
}

package com.journeyassistant.api;

import com.journeyassistant.journey.InvalidJourneyTransitionException;
import com.journeyassistant.journey.JourneyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                                  HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request);
    }

    @ExceptionHandler(JourneyNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(JourneyNotFoundException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidJourneyTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidJourneyTransitionException exception,
                                                                     HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}

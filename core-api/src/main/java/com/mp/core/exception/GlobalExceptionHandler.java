package com.mp.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * จัดการ exceptions ทั้งหมดที่เกิดขึ้นใน application แบบรวมศูนย์
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle Custom Business Exceptions (BaseException และ subclasses)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            HttpServletRequest request
    ) {
        log.error("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /**
     * Handle Validation Errors (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        });

        log.warn("Validation failed for request {}: {}", request.getRequestURI(), validationErrors);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle Missing Path Variable
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariable(
            MissingPathVariableException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing path variable: {}", ex.getVariableName());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                String.format("Required path variable '%s' is missing", ex.getVariableName()),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle Database Constraint Violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Database constraint violation";

        // Extract meaningful message from SQLException
        if (ex.getRootCause() instanceof SQLException sqlEx) {
            String sqlMessage = sqlEx.getMessage().toLowerCase();

            if (sqlMessage.contains("duplicate") || sqlMessage.contains("unique")) {
                message = "A record with this value already exists";
            } else if (sqlMessage.contains("foreign key")) {
                message = "Cannot delete record because it is referenced by other records";
            }
        }

        log.error("Data integrity violation: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle Access Denied (Spring Security)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied for request: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle HTTP Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Method not supported: {} for {}", ex.getMethod(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED,
                String.format("Method %s is not supported for this endpoint", ex.getMethod()),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handle Missing Request Parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle Type Mismatch (e.g., String to Long conversion error)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("Type mismatch: {}", message);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle Generic Exceptions (Fallback)
     * จับ Exception ทั้งหมดที่ไม่ถูกจับโดย handlers อื่น
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

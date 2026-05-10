package com.mp.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard Error Response DTO
 * ใช้สำหรับ return error response ในรูปแบบที่สม่ำเสมอ
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;

    /**
     * Factory method สำหรับสร้าง ErrorResponse แบบง่าย
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method สำหรับสร้าง ErrorResponse พร้อม validation errors
     */
    public static ErrorResponse withValidationErrors(
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
    }
}

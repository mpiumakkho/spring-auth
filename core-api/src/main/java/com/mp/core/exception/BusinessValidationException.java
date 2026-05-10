package com.mp.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับ Business Logic Validation Errors
 * ส่ง HTTP Status 400 Bad Request
 */
public class BusinessValidationException extends BaseException {

    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public BusinessValidationException(String message, Throwable cause) {
        super(message, "BUSINESS_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}

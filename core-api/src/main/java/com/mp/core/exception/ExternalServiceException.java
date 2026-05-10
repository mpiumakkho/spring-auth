package com.mp.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับกรณีที่ External Service ไม่ตอบสนอง หรือมีปัญหา
 * ส่ง HTTP Status 503 Service Unavailable
 */
public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String serviceName, String reason) {
        super(
                String.format("External service '%s' is unavailable: %s", serviceName, reason),
                "EXTERNAL_SERVICE_ERROR",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super(
                String.format("External service '%s' is unavailable", serviceName),
                "EXTERNAL_SERVICE_ERROR",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}

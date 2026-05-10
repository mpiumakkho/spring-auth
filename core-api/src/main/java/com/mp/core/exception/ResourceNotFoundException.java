package com.mp.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับกรณีที่หา Resource ไม่เจอ
 * ส่ง HTTP Status 404 Not Found
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                String.format("%s with identifier '%s' not found", resourceName, identifier),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(
                String.format("%s with id %d not found", resourceName, id),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

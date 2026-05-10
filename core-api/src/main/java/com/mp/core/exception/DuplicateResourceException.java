package com.mp.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับกรณีที่พยายามสร้าง Resource ที่มีอยู่แล้ว
 * ส่ง HTTP Status 409 Conflict
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resourceName, String field, String value) {
        super(
                String.format("%s with %s '%s' already exists", resourceName, field, value),
                "DUPLICATE_RESOURCE",
                HttpStatus.CONFLICT
        );
    }

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }
}

package com.mp.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base Exception Class สำหรับ Custom Exceptions ทั้งหมด
 * ช่วยให้ Exception มี structure ที่เป็นมาตรฐาน
 */
@Getter
public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    protected BaseException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    protected BaseException(String message, String errorCode, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }
}

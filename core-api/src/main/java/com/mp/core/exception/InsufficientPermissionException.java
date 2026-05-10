package com.mp.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับกรณีที่ user ไม่มีสิทธิ์เข้าถึง resource
 * ส่ง HTTP Status 403 Forbidden
 */
public class InsufficientPermissionException extends BaseException {

    public InsufficientPermissionException(String action) {
        super(
                String.format("You do not have permission to %s", action),
                "INSUFFICIENT_PERMISSION",
                HttpStatus.FORBIDDEN
        );
    }

    public InsufficientPermissionException(String message, String action) {
        super(message, "INSUFFICIENT_PERMISSION", HttpStatus.FORBIDDEN);
    }
}

package com.mp.web.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown for client-side validation errors.
 * Contains field-specific error messages for form validation.
 */
public class ValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;
    private final String redirectPath;

    public ValidationException(
            String message,
            Map<String, String> fieldErrors,
            String redirectPath
    ) {
        super(message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
        this.redirectPath = redirectPath;
    }

    public ValidationException(String message, String redirectPath) {
        super(message);
        this.fieldErrors = new HashMap<>();
        this.redirectPath = redirectPath;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}

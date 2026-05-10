package com.mp.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when core-api returns an error response.
 * Wraps HTTP errors from RestTemplate calls to core-api.
 */
public class CoreApiClientException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String errorCode;
    private final String redirectPath;

    public CoreApiClientException(
            String message,
            HttpStatus statusCode,
            String errorCode,
            String redirectPath
    ) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.redirectPath = redirectPath;
    }

    public CoreApiClientException(
            String message,
            HttpStatus statusCode,
            String redirectPath,
            Throwable cause
    ) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = null;
        this.redirectPath = redirectPath;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public boolean isClientError() {
        return statusCode != null && statusCode.is4xxClientError();
    }

    public boolean isServerError() {
        return statusCode != null && statusCode.is5xxServerError();
    }
}

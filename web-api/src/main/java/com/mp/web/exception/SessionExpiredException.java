package com.mp.web.exception;

/**
 * Exception thrown when user session has expired.
 * Redirects user to login page with appropriate message.
 */
public class SessionExpiredException extends RuntimeException {
    private final String loginPath;

    public SessionExpiredException() {
        super("Your session has expired. Please login again.");
        this.loginPath = "/login";
    }

    public SessionExpiredException(String message) {
        super(message);
        this.loginPath = "/login";
    }

    public SessionExpiredException(String message, String loginPath) {
        super(message);
        this.loginPath = loginPath;
    }

    public String getLoginPath() {
        return loginPath;
    }
}

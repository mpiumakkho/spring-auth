package com.mp.core.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BaseException {

    public AccountLockedException(String message) {
        super(message, "ACCOUNT_LOCKED", HttpStatus.LOCKED);
    }
}

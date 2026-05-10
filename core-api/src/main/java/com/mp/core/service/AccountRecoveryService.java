package com.mp.core.service;

public interface AccountRecoveryService {

    /** Issue a new verification token for the user's email and send it. */
    void issueEmailVerification(String userId);

    /** Consume an email verification token, mark user's email as verified (status=ACTIVE). */
    void verifyEmail(String token);

    /** Issue a password-reset token for the given email (silently no-op if email not found). */
    void requestPasswordReset(String email);

    /** Consume a password-reset token and set a new password (validated against policy). */
    void resetPassword(String token, String newPassword);
}

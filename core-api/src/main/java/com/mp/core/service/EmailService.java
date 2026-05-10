package com.mp.core.service;

public interface EmailService {

    void sendEmailVerification(String to, String username, String verificationLink);

    void sendPasswordReset(String to, String username, String resetLink);
}

package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.mp.core.service.EmailService;

/**
 * SMTP-backed email service. When mail is unconfigured (dev) the JavaMailSender
 * may be null; in that case we log the message instead of throwing.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@rbac-ums.local}")
    private String from;

    @Override
    public void sendEmailVerification(String to, String username, String verificationLink) {
        String subject = "Please verify your email";
        String body = String.format(
                "Hi %s,%n%nPlease verify your email by clicking: %s%n%n(Link expires in 24 hours.)",
                username, verificationLink);
        send(to, subject, body);
    }

    @Override
    public void sendPasswordReset(String to, String username, String resetLink) {
        String subject = "Password reset requested";
        String body = String.format(
                "Hi %s,%n%nReset your password using this link: %s%n%n(Link expires in 1 hour.)",
                username, resetLink);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("[mail-stub] to={} subject={} body={}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

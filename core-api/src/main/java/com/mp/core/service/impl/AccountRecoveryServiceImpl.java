package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.EmailVerification;
import com.mp.core.entity.PasswordReset;
import com.mp.core.entity.User;
import com.mp.core.entity.UserStatus;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.EmailVerificationRepository;
import com.mp.core.repository.PasswordResetRepository;
import com.mp.core.repository.UserRepository;
import com.mp.core.service.AccountRecoveryService;
import com.mp.core.service.AuditService;
import com.mp.core.service.EmailService;
import com.mp.core.service.NotificationService;
import com.mp.core.validation.PasswordPolicyValidator;

@Slf4j
@Service
public class AccountRecoveryServiceImpl implements AccountRecoveryService {

    private final UserRepository userRepo;
    private final EmailVerificationRepository emailRepo;
    private final PasswordResetRepository resetRepo;
    private final EmailService emailService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PasswordEncoder encoder;

    @Value("${core.security.email-verification-ttl-hours:24}")
    private int emailTtlHours;

    @Value("${core.security.password-reset-ttl-minutes:60}")
    private int resetTtlMinutes;

    @Value("${core.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public AccountRecoveryServiceImpl(
            UserRepository userRepo,
            EmailVerificationRepository emailRepo,
            PasswordResetRepository resetRepo,
            EmailService emailService,
            AuditService auditService,
            NotificationService notificationService,
            PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.emailRepo = emailRepo;
        this.resetRepo = resetRepo;
        this.emailService = emailService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void issueEmailVerification(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        EmailVerification ev = new EmailVerification();
        ev.setUserId(userId);
        ev.setToken(UUID.randomUUID().toString());
        ev.setExpiresAt(plusHours(new Date(), emailTtlHours));
        emailRepo.save(ev);

        String link = appBaseUrl + "/verify-email?token=" + ev.getToken();
        emailService.sendEmailVerification(user.getEmail(), user.getUsername(), link);
        auditService.log(user.getUsername(), "EMAIL_VERIFY_ISSUED", "USER", userId, "to=" + user.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerification ev = emailRepo.findByToken(token)
                .orElseThrow(() -> new BusinessValidationException("Invalid verification token"));
        if (ev.isConsumed()) {
            throw new BusinessValidationException("Token already used");
        }
        if (ev.getExpiresAt().before(new Date())) {
            throw new BusinessValidationException("Token expired");
        }

        User user = userRepo.findById(ev.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", ev.getUserId()));
        user.setStatus(UserStatus.ACTIVE);
        userRepo.save(user);

        ev.setConsumed(true);
        emailRepo.save(ev);
        auditService.log(user.getUsername(), "EMAIL_VERIFIED", "USER", user.getUserId(), null);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        // Silently succeed even if email not found, to avoid account enumeration
        userRepo.findByEmail(email).ifPresent(user -> {
            PasswordReset pr = new PasswordReset();
            pr.setUserId(user.getUserId());
            pr.setToken(UUID.randomUUID().toString());
            pr.setExpiresAt(plusMinutes(new Date(), resetTtlMinutes));
            resetRepo.save(pr);

            String link = appBaseUrl + "/reset-password?token=" + pr.getToken();
            emailService.sendPasswordReset(user.getEmail(), user.getUsername(), link);
            auditService.log(user.getUsername(), "PASSWORD_RESET_ISSUED", "USER", user.getUserId(), "to=" + email);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (!PasswordPolicyValidator.check(newPassword)) {
            throw new BusinessValidationException(
                    "Password must be at least 8 characters and include uppercase, lowercase, digit, and special character");
        }
        PasswordReset pr = resetRepo.findByToken(token)
                .orElseThrow(() -> new BusinessValidationException("Invalid reset token"));
        if (pr.isConsumed()) {
            throw new BusinessValidationException("Token already used");
        }
        if (pr.getExpiresAt().before(new Date())) {
            throw new BusinessValidationException("Token expired");
        }

        User user = userRepo.findById(pr.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", pr.getUserId()));
        user.setPassword(encoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);

        pr.setConsumed(true);
        resetRepo.save(pr);
        auditService.log(user.getUsername(), "PASSWORD_RESET", "USER", user.getUserId(), null);
        notificationService.notify(user.getUserId(), "PASSWORD_RESET",
                "Your password was changed",
                "Your password has been reset. If this wasn't you, contact an administrator.");
    }

    private static Date plusHours(Date base, int hours) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();
    }

    private static Date plusMinutes(Date base, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }
}

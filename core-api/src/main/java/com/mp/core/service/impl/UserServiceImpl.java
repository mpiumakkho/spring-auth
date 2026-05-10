package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Role;
import com.mp.core.entity.User;
import com.mp.core.entity.UserStatus;
import com.mp.core.exception.AccountLockedException;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.DuplicateResourceException;
import com.mp.core.exception.InvalidCredentialsException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.RoleRepository;
import com.mp.core.repository.UserRepository;
import com.mp.core.service.AuditService;
import com.mp.core.service.NotificationService;
import com.mp.core.service.UserService;

@Slf4j
@Service
public class UserServiceImpl implements UserService {    
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    public UserServiceImpl(
            UserRepository userRepo,
            RoleRepository roleRepo,
            PasswordEncoder encoder,
            AuditService auditService,
            NotificationService notificationService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            log.warn("Username {} is already taken", user.getUsername());
            throw new DuplicateResourceException("User", "username", user.getUsername());
        }
        
        String email = user.getEmail();
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessValidationException("Invalid email format");
        }
        
        if (userRepo.existsByEmail(email)) {
            log.warn("Email {} is already registered", email);
            throw new DuplicateResourceException("User", "email", email);
        }

        if (user.getPassword() != null) {
            user.setPassword(encoder.encode(user.getPassword()));
        }

        user.setStatus(UserStatus.PENDING);
        
        log.info("Creating new user account for: {}", user.getUsername());
        User saved = userRepo.save(user);
        auditService.log(user.getCreatedBy(), "CREATE", "USER", saved.getUserId(), "Created user: " + saved.getUsername());
        return saved;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        User existing = userRepo.findById(user.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", user.getUserId()));
        
        String newUsername = user.getUsername();
        if (!existing.getUsername().equals(newUsername)) {
            if (userRepo.existsByUsername(newUsername)) {
                throw new DuplicateResourceException("User", "username", newUsername);
            }
            existing.setUsername(newUsername);
        }
        
        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        
        String newPassword = user.getPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            existing.setPassword(encoder.encode(newPassword));
        }
        
        existing.setUpdatedBy(user.getUpdatedBy());
        
        log.debug("Updating user: {}", existing.getUserId());
        User updated = userRepo.save(existing);
        auditService.log(user.getUpdatedBy(), "UPDATE", "USER", updated.getUserId(), "Updated user: " + updated.getUsername());
        return updated;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userRoles", key = "#id")
    public void deleteUser(String id) {
        if (!userRepo.existsById(id)) {
            log.warn("Attempted to delete non-existent user: {}", id);
            throw new ResourceNotFoundException("User", id);
        }
        
        userRepo.deleteById(id);
        auditService.log(null, "DELETE", "USER", id, "Deleted user");
        log.info("User {} deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(String id) {
        return userRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userRoles", key = "#userId")
    public void assignRoleToUser(String userId, String roleId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        if (user.getRoles().contains(role)) {
            log.info("User {} already has role {}", userId, roleId);
            return;
        }

        user.getRoles().add(role);
        userRepo.save(user);
        auditService.log(null, "ASSIGN_ROLE", "USER", userId, "Assigned role " + role.getName() + " to user");
        notificationService.notify(userId, "ROLE_ASSIGNED",
                "New role assigned",
                "You have been granted role: " + role.getName());
        log.info("Role {} assigned to user {}", roleId, userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userRoles", key = "#userId")
    public void removeRoleFromUser(String userId, String roleId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        if (user.getRoles().remove(role)) {
            userRepo.save(user);
            auditService.log(null, "REMOVE_ROLE", "USER", userId, "Removed role " + role.getName() + " from user");
            notificationService.notify(userId, "ROLE_REMOVED",
                    "Role removed",
                    "Role " + role.getName() + " has been removed from your account");
            log.info("Role {} removed from user {}", roleId, userId);
        } else {
            log.warn("User {} did not have role {}", userId, roleId);
        }
    }

    @Override
    @Transactional
    public User updateUserStatus(String userId, String status) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            
        if (!UserStatus.isValid(status)) {
            throw new BusinessValidationException("Invalid status: " + status);
        }

        user.setStatus(status);
        
        return userRepo.save(user);
    }

    @Override
    @Transactional
    public User activateUser(String userId) {
        return updateUserStatus(userId, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public User deactivateUser(String userId) {
        return updateUserStatus(userId, UserStatus.INACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepo.findByStatus(UserStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(String status) {
        if (!UserStatus.isValid(status)) {
            throw new BusinessValidationException("Invalid status filter: " + status);
        }
        return userRepo.findByStatus(status);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    @Override
    @Transactional
    public User authenticate(String identifier, String rawPassword, String ipAddress) {
        User user = userRepo.findByUsername(identifier)
                .or(() -> userRepo.findByEmail(identifier))
                .orElseThrow(InvalidCredentialsException::new);

        Date now = new Date();
        if (user.getLockedUntil() != null && user.getLockedUntil().after(now)) {
            throw new AccountLockedException("Account is locked. Try again after " + user.getLockedUntil());
        }

        if (!encoder.matches(rawPassword, user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            String detail;
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                cal.add(Calendar.MINUTE, LOCKOUT_MINUTES);
                user.setLockedUntil(cal.getTime());
                user.setFailedLoginAttempts(0);
                detail = "Account locked after " + MAX_LOGIN_ATTEMPTS + " failed attempts (ip=" + ipAddress + ")";
                userRepo.save(user);
                auditService.log(user.getUsername(), "ACCOUNT_LOCKED", "USER", user.getUserId(), detail);
                notificationService.notify(user.getUserId(), "ACCOUNT_LOCKED",
                        "Your account has been locked",
                        "Too many failed login attempts. Try again in " + LOCKOUT_MINUTES + " minutes.");
                throw new AccountLockedException("Account locked due to too many failed login attempts. Try again in "
                        + LOCKOUT_MINUTES + " minutes.");
            }
            userRepo.save(user);
            auditService.log(user.getUsername(), "LOGIN_FAILED", "USER", user.getUserId(),
                    "Failed login attempt " + attempts + " (ip=" + ipAddress + ")");
            throw new InvalidCredentialsException();
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessValidationException("Account is not active: status=" + user.getStatus());
        }

        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepo.save(user);
        }
        auditService.log(user.getUsername(), "LOGIN_SUCCESS", "USER", user.getUserId(), "ip=" + ipAddress);
        return user;
    }

    @Override
    @Transactional
    public User unlockAccount(String userId, String actor) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = userRepo.save(user);
        auditService.log(actor, "UNLOCK", "USER", userId, "Manually unlocked user: " + user.getUsername());
        notificationService.notify(userId, "ACCOUNT_UNLOCKED",
                "Your account has been unlocked",
                "An administrator has unlocked your account.");
        return saved;
    }

    @Override
    @Transactional
    public User updateProfile(String userId, String firstName, String lastName, String phone, String bio) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        if (bio != null) user.setBio(bio);
        User saved = userRepo.save(user);
        auditService.log(user.getUsername(), "UPDATE_PROFILE", "USER", userId, "Profile updated");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userRoles", key = "#userId")
    public java.util.Set<String> getUserRoleNames(String userId) {
        return userRepo.findById(userId)
                .map(u -> u.getRoles() == null
                        ? java.util.Set.<String>of()
                        : u.getRoles().stream()
                                .map(com.mp.core.entity.Role::getName)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet()))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Override
    @Transactional
    public User updateAvatarUrl(String userId, String avatarUrl) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setAvatarUrl(avatarUrl);
        User saved = userRepo.save(user);
        auditService.log(user.getUsername(), "UPDATE_AVATAR", "USER", userId, "Avatar URL: " + avatarUrl);
        return saved;
    }
}


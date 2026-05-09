package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Role;
import com.mp.core.entity.User;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.DuplicateResourceException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.RoleRepository;
import com.mp.core.repository.UserRepository;
import com.mp.core.service.AuditService;
import com.mp.core.service.UserService;

@Slf4j
@Service
public class UserServiceImpl implements UserService {    
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AuditService auditService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    public UserServiceImpl(
            UserRepository userRepo,
            RoleRepository roleRepo,
            PasswordEncoder encoder,
            AuditService auditService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.auditService = auditService;
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

        user.setStatus("pending");
        
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
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepo.findAll(pageable);
    }

    @Override
    @Transactional
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
        log.info("Role {} assigned to user {}", roleId, userId);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(String userId, String roleId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        if (user.getRoles().remove(role)) {
            userRepo.save(user);
            auditService.log(null, "REMOVE_ROLE", "USER", userId, "Removed role " + role.getName() + " from user");
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
            
        if (!isValidStatus(status)) {
            throw new BusinessValidationException("Invalid status: " + status);
        }

        user.setStatus(status);
        
        return userRepo.save(user);
    }

    private boolean isValidStatus(String status) {
        return status != null && List.of("active", "inactive", "pending", "locked").contains(status);
    }

    @Override
    @Transactional
    public User activateUser(String userId) {
        return updateUserStatus(userId, "active");
    }

    @Override
    @Transactional
    public User deactivateUser(String userId) {
        return updateUserStatus(userId, "inactive");
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepo.findByStatus("pending");
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(String status) {
        if (!isValidStatus(status)) {
            throw new BusinessValidationException("Invalid status filter: " + status);
        }
        return userRepo.findByStatus(status);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }
} 
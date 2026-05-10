package com.mp.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mp.core.entity.User;

public interface UserService {
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(String id);
    Optional<User> getUserById(String id);
    Optional<User> getUserByUsername(String username);
    Page<User> getAllUsers(Pageable pageable);
    void assignRoleToUser(String userId, String roleId);
    void removeRoleFromUser(String userId, String roleId);
    User updateUserStatus(String userId, String status);
    
    // admin functions for user management
    User activateUser(String userId);
    User deactivateUser(String userId);
    List<User> getPendingUsers();
    List<User> getUsersByStatus(String status);
    Optional<User> getUserByEmail(String email);

    /**
     * Authenticate username/email + raw password, applying account-lockout policy.
     * On success returns the user and resets failed attempts.
     * On failure increments attempts and locks the account when the threshold is reached.
     * @return the authenticated user, never null. Throws on invalid credentials, locked account, or inactive user.
     */
    User authenticate(String identifier, String rawPassword, String ipAddress);

    User unlockAccount(String userId, String actor);

    User updateProfile(String userId, String firstName, String lastName, String phone, String bio);

    User updateAvatarUrl(String userId, String avatarUrl);
}

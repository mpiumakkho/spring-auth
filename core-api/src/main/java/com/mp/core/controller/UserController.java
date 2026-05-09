package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.UserMapper;
import com.mp.core.dto.UserRequestDTO;
import com.mp.core.entity.Role;
import com.mp.core.entity.User;
import com.mp.core.entity.UserSession;
import com.mp.core.service.UserService;
import com.mp.core.service.UserSessionService;
import com.mp.core.util.RoleEncryptor;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PasswordEncoder pwdEncoder;
    private final UserSessionService sessionService;

    @Autowired
    private RoleEncryptor roleEncryptor;

    public UserController(UserService userService, PasswordEncoder pwdEncoder, UserSessionService sessionService) {
        this.userService = userService;
        this.pwdEncoder = pwdEncoder;
        this.sessionService = sessionService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "createdAt,desc") String sort) {
        try {
            String[] sortParams = sort.split(",");
            org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(
                sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1])
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC,
                sortParams[0]
            );
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sortObj);
            org.springframework.data.domain.Page<User> userPage = userService.getAllUsers(pageable);

            org.springframework.data.domain.Page<com.mp.core.dto.UserResponseDTO> dtoPage = userPage.map(UserMapper::toUserResponseDTO);
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.internalServerError()
                .body("Something went wrong while fetching users");
        }
    }

    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    @PostMapping("/find-by-id")
    public ResponseEntity<?> getUserById(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String userId = json.optString("userId");
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID required");
            }

            Optional<User> user = userService.getUserById(userId);
            if (user.isPresent()) {
                return ResponseEntity.ok(UserMapper.toUserResponseDTO(user.get()));
            } else {
                return ResponseEntity.status(404).body("User not found with ID: " + userId);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error finding user: " + e.getMessage());
        }
    }

    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    @PostMapping("/find-by-username")
    public ResponseEntity<?> getUserByUsername(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String username = json.optString("username");
            
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username required");
            }

            Optional<User> user = userService.getUserByUsername(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(UserMapper.toUserResponseDTO(user.get()));
            } else {
                return ResponseEntity.status(404).body("User not found with username: " + username);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error finding user by username: " + e.getMessage());
        }
    }

    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    @PostMapping("/find-by-username-or-email")
    public ResponseEntity<?> getUserByUsernameOrEmail(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String usernameOrEmail = json.optString("username");
            if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username or email required");
            }
            Optional<User> user = userService.getUserByUsername(usernameOrEmail);
            if (!user.isPresent()) {
                user = userService.getUserByEmail(usernameOrEmail);
            }
            if (user.isPresent()) {
                User u = user.get();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", u.getUserId());
                userJson.put("username", u.getUsername());
                userJson.put("email", u.getEmail());
                userJson.put("firstName", u.getFirstName());
                userJson.put("lastName", u.getLastName());
                userJson.put("status", u.getStatus());
                List<String> roleNames = new ArrayList<>();
                if (u.getRoles() != null) {
                    u.getRoles().forEach(role -> roleNames.add(role.getName()));
                }
                userJson.put("roles", roleNames);
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("user", userJson);
                return ResponseEntity.ok(result.toString());
            } else {
                JSONObject fail = new JSONObject();
                fail.put("success", false);
                fail.put("message", "User not found with username or email: " + usernameOrEmail);
                return ResponseEntity.status(404).body(fail.toString());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error finding user by username or email: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'USER:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody User newUser) {
        if (newUser == null) {
            return ResponseEntity.badRequest().body("Invalid user data");
        }

        String username = newUser.getUsername();
        String email = newUser.getEmail();

        if (username == null || username.isBlank()) {
            log.warn("Attempt to create user with empty username");
            return ResponseEntity.badRequest().body("Username cannot be empty");
        }

        if (email == null || !email.contains("@")) {
            log.warn("Invalid email format: {}", email);
            return ResponseEntity.badRequest().body("Please provide a valid email");
        }

        try {
            log.info("Creating new user: {}", username);
            User user = userService.createUser(newUser);
            log.debug("User created successfully with ID: {}", user.getUserId());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("User creation failed for {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error while creating user: " + username, e);
            return ResponseEntity.internalServerError()
                .body("Unable to create user at this time");
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        String userId = user.getUserId();
        
        if (userId == null || userId.isBlank()) {
            log.warn("Update attempted without user ID");
            return ResponseEntity.badRequest().body("Missing user ID");
        }

        try {
            if (!userService.getUserById(userId).isPresent()) {
                log.info("Update attempted for non-existent user: {}", userId);
                return ResponseEntity.notFound().build();
            }

            User updated = userService.updateUser(user);
            log.info("User {} updated successfully", userId);
            return ResponseEntity.ok(updated);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid data for user update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            log.error("Failed to update user " + userId, e);
            return ResponseEntity.internalServerError()
                .body("Update failed - please try again later");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("Invalid user ID");
        }

        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                log.info("Delete attempted for non-existent user: {}", userId);
                return ResponseEntity.notFound().build();
            }

            userService.deleteUser(userId);
            log.info("User {} deleted successfully", userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error occurred while deleting user: " + userId, e);
            return ResponseEntity.internalServerError()
                .body("Failed to delete user - please try again");
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'USER:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserPost(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String userId = json.optString("userId");
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID required");
            }

            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                log.info("Delete attempted for non-existent user: {}", userId);
                return ResponseEntity.notFound().build();
            }

            userService.deleteUser(userId);
            log.info("User {} deleted successfully", userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error occurred while deleting user", e);
            return ResponseEntity.internalServerError()
                .body("Failed to delete user - please try again");
        }
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<?> assignRoleToUser(@RequestBody String request) {
        String userId = null;
        String roleId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            userId = json.optString("userId");
            roleId = json.optString("roleId");
            
            if (userId == null || userId.trim().isEmpty() || roleId == null || roleId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID and Role ID required");
            }

            userService.assignRoleToUser(userId, roleId);
            return ResponseEntity.ok("Role assigned successfully to user ID: " + userId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(404).body("User not found with ID: " + userId);
            } else if (e.getMessage().contains("Role not found")) {
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            } else {
                return ResponseEntity.badRequest().body("Role assignment failed: " + e.getMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error assigning role: " + e.getMessage());
        }
    }

    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    @PostMapping("/remove-role")
    public ResponseEntity<?> removeRoleFromUser(@RequestBody String request) {
        String userId = null;
        String roleId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            userId = json.optString("userId");
            roleId = json.optString("roleId");
            
            if (userId == null || userId.trim().isEmpty() || roleId == null || roleId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID and Role ID required");
            }

            userService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.ok("Role removed successfully from user ID: " + userId);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(404).body("User not found with ID: " + userId);
            } else if (e.getMessage().contains("Role not found")) {
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            } else {
                return ResponseEntity.badRequest().body("Role removal failed: " + e.getMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing role: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/update-status")
    public ResponseEntity<?> updateUserStatus(@RequestBody String request) {
        String userId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            userId = json.optString("userId");
            String status = json.optString("status");
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID required");
            }
            
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status required");
            }

            User updatedUser = userService.updateUserStatus(userId, status);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("User not found with ID: " + userId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating user status: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/activate")
    public ResponseEntity<?> activateUser(@Valid @RequestBody UserRequestDTO request) {
        String userId = request.getUserId();
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID required");
            }
            User activatedUser = userService.activateUser(userId);
            return ResponseEntity.ok(UserMapper.toUserResponseDTO(activatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    @PostMapping("/admin/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@RequestBody String request) {
        String userId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            userId = json.optString("userId");
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User ID required");
            }

            User deactivatedUser = userService.deactivateUser(userId);
            return ResponseEntity.ok(deactivatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("User not found with ID: " + userId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deactivating user: " + e.getMessage());
        }
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingUsers() {
        try {
            List<User> pendingUsers = userService.getPendingUsers();
            return ResponseEntity.ok(pendingUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting pending users: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users-by-status")
    public ResponseEntity<?> getUsersByStatus(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String status = json.optString("status");
            
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status required");
            }

            List<User> users = userService.getUsersByStatus(status);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting users by status: " + e.getMessage());
        }
    }

    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    @PostMapping("/find")
    public ResponseEntity<?> findUser(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String userId = json.optString("userId");
            String username = json.optString("username");
            
            if (userId != null && !userId.trim().isEmpty()) {
                Optional<User> user = userService.getUserById(userId);
                if (user.isPresent()) {
                    return ResponseEntity.ok(user.get());
                } else {
                    return ResponseEntity.status(404).body("User not found with ID: " + userId);
                }
            }
            
            if (username != null && !username.trim().isEmpty()) {
                Optional<User> user = userService.getUserByUsername(username);
                if (user.isPresent()) {
                    return ResponseEntity.ok(user.get());
                } else {
                    return ResponseEntity.status(404).body("User not found with username: " + username);
                }
            }
            
            return ResponseEntity.badRequest().body("Either userId or username required");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error finding user: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String usernameOrEmail = json.optString("username");
            String password = json.optString("password");

            Optional<User> userOpt = userService.getUserByUsername(usernameOrEmail);
            if (!userOpt.isPresent()) {
                userOpt = userService.getUserByEmail(usernameOrEmail);
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean match = pwdEncoder.matches(password, user.getPassword());
                if (match) {
                    // Create new session
                    UserSession session = sessionService.createSession(user.getUserId());
                    
                    JSONObject userJson = new JSONObject();
                    userJson.put("userId", user.getUserId());
                    userJson.put("username", user.getUsername());
                    userJson.put("email", user.getEmail());
                    userJson.put("firstName", user.getFirstName());
                    userJson.put("lastName", user.getLastName());
                    userJson.put("status", user.getStatus());
                    List<JSONObject> loginRoles = new ArrayList<>();
                    if (user.getRoles() != null) {
                        for (Role role : user.getRoles()) {
                            JSONObject r = new JSONObject();
                            r.put("roleId", role.getRoleId());
                            r.put("name", role.getName());
                            loginRoles.add(r);
                        }
                    }
                    userJson.put("roles", loginRoles);
                    userJson.put("token", session.getToken());
                    
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("user", userJson);
                    return ResponseEntity.ok(result.toString());
                }
            }
            JSONObject fail = new JSONObject();
            fail.put("success", false);
            fail.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(fail.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Login error: " + e.getMessage());
        }
    }

    @PostMapping("/login-encrypt")
    public ResponseEntity<?> loginEncrypt(@Valid @RequestBody UserRequestDTO request) {
        try {
            String usernameOrEmail = request.getUsername();
            String password = request.getPassword();
            Optional<User> userOpt = userService.getUserByUsername(usernameOrEmail);
            if (!userOpt.isPresent()) {
                userOpt = userService.getUserByEmail(usernameOrEmail);
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean match = pwdEncoder.matches(password, user.getPassword());
                if (match) {
                    java.util.List<Role> roles = new java.util.ArrayList<>(user.getRoles());
                    String encryptedRoles = roleEncryptor.encryptRoles(roles);
                    JSONObject result = new JSONObject();
                    result.put("userId", user.getUserId());
                    result.put("username", user.getUsername());
                    result.put("email", user.getEmail());
                    result.put("roles_encrypted", encryptedRoles);
                    return ResponseEntity.ok(result.toString());
                }
            }
            return ResponseEntity.status(401).body("Invalid credentials");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

} 
package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.AssignRoleRequestDTO;
import com.mp.core.dto.CreateUserRequestDTO;
import com.mp.core.dto.FindUserRequestDTO;
import com.mp.core.dto.LoginRequestDTO;
import com.mp.core.dto.StatusFilterRequestDTO;
import com.mp.core.dto.UpdateUserRequestDTO;
import com.mp.core.dto.UpdateUserStatusRequestDTO;
import com.mp.core.dto.UserIdRequestDTO;
import com.mp.core.dto.UserMapper;
import com.mp.core.dto.UserResponseDTO;
import com.mp.core.dto.UsernameRequestDTO;
import com.mp.core.entity.Role;
import com.mp.core.entity.User;
import com.mp.core.entity.UserSession;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
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
    private final RoleEncryptor roleEncryptor;

    public UserController(
            UserService userService,
            PasswordEncoder pwdEncoder,
            UserSessionService sessionService,
            RoleEncryptor roleEncryptor) {
        this.userService = userService;
        this.pwdEncoder = pwdEncoder;
        this.sessionService = sessionService;
        this.roleEncryptor = roleEncryptor;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<UserResponseDTO> dtoPage = userService.getAllUsers(pageable).map(UserMapper::toUserResponseDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/find-by-id")
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@Valid @RequestBody UserIdRequestDTO request) {
        User user = userService.getUserById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(user));
    }

    @PostMapping("/find-by-username")
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@Valid @RequestBody UsernameRequestDTO request) {
        User user = userService.getUserByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + request.username() + "' not found"));
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(user));
    }

    @PostMapping("/find-by-username-or-email")
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<String> getUserByUsernameOrEmail(@Valid @RequestBody UsernameRequestDTO request) {
        String identifier = request.username();
        Optional<User> userOpt = userService.getUserByUsername(identifier)
                .or(() -> userService.getUserByEmail(identifier));

        if (userOpt.isEmpty()) {
            JSONObject fail = new JSONObject();
            fail.put("success", false);
            fail.put("message", "User not found with username or email: " + identifier);
            return ResponseEntity.status(404).body(fail.toString());
        }

        User u = userOpt.get();
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
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'USER:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setPassword(request.password());
        newUser.setEmail(request.email());
        newUser.setFirstName(request.firstName());
        newUser.setLastName(request.lastName());
        User created = userService.createUser(newUser);
        log.info("User created: {} (id={})", created.getUsername(), created.getUserId());
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(created));
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@Valid @RequestBody UpdateUserRequestDTO request) {
        User updates = new User();
        updates.setUserId(request.userId());
        updates.setUsername(request.username());
        updates.setFirstName(request.firstName());
        updates.setLastName(request.lastName());
        updates.setPassword(request.password());
        User updated = userService.updateUser(updates);
        log.info("User updated: {}", updated.getUserId());
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'USER:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String userId) {
        userService.deleteUser(userId);
        log.info("User {} deleted successfully", userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'USER:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserPost(@Valid @RequestBody UserIdRequestDTO request) {
        userService.deleteUser(request.userId());
        log.info("User {} deleted successfully", request.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<String> assignRoleToUser(@Valid @RequestBody AssignRoleRequestDTO request) {
        userService.assignRoleToUser(request.userId(), request.roleId());
        return ResponseEntity.ok("Role assigned successfully to user ID: " + request.userId());
    }

    @PostMapping("/remove-role")
    @PreAuthorize("hasPermission(null, 'USER:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<String> removeRoleFromUser(@Valid @RequestBody AssignRoleRequestDTO request) {
        userService.removeRoleFromUser(request.userId(), request.roleId());
        return ResponseEntity.ok("Role removed successfully from user ID: " + request.userId());
    }

    @PostMapping("/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserStatus(@Valid @RequestBody UpdateUserStatusRequestDTO request) {
        User updated = userService.updateUserStatus(request.userId(), request.status());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/admin/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> activateUser(@Valid @RequestBody UserIdRequestDTO request) {
        User activated = userService.activateUser(request.userId());
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(activated));
    }

    @PostMapping("/admin/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> deactivateUser(@Valid @RequestBody UserIdRequestDTO request) {
        User deactivated = userService.deactivateUser(request.userId());
        return ResponseEntity.ok(deactivated);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    @PostMapping("/admin/users-by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByStatus(@Valid @RequestBody StatusFilterRequestDTO request) {
        return ResponseEntity.ok(userService.getUsersByStatus(request.status()));
    }

    @PostMapping("/find")
    @PreAuthorize("hasPermission(null, 'USER:READ') or hasRole('ADMIN')")
    public ResponseEntity<User> findUser(@RequestBody FindUserRequestDTO request) {
        String userId = request.userId();
        String username = request.username();

        if (userId != null && !userId.isBlank()) {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return ResponseEntity.ok(user);
        }

        if (username != null && !username.isBlank()) {
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' not found"));
            return ResponseEntity.ok(user);
        }

        throw new BusinessValidationException("Either userId or username is required");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO request) {
        String identifier = request.username();
        Optional<User> userOpt = userService.getUserByUsername(identifier)
                .or(() -> userService.getUserByEmail(identifier));

        if (userOpt.isPresent() && pwdEncoder.matches(request.password(), userOpt.get().getPassword())) {
            User user = userOpt.get();
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

        JSONObject fail = new JSONObject();
        fail.put("success", false);
        fail.put("message", "Invalid username or password");
        return ResponseEntity.status(401).body(fail.toString());
    }

    @PostMapping("/login-encrypt")
    public ResponseEntity<String> loginEncrypt(@Valid @RequestBody LoginRequestDTO request) throws Exception {
        String identifier = request.username();
        Optional<User> userOpt = userService.getUserByUsername(identifier)
                .or(() -> userService.getUserByEmail(identifier));

        if (userOpt.isPresent() && pwdEncoder.matches(request.password(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            List<Role> roles = new ArrayList<>(user.getRoles());
            String encryptedRoles = roleEncryptor.encryptRoles(roles);
            JSONObject result = new JSONObject();
            result.put("userId", user.getUserId());
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            result.put("roles_encrypted", encryptedRoles);
            return ResponseEntity.ok(result.toString());
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}

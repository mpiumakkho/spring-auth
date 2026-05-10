package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mp.core.dto.UpdateProfileRequestDTO;
import com.mp.core.dto.UserMapper;
import com.mp.core.dto.UserResponseDTO;
import com.mp.core.entity.User;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.service.UserService;

import jakarta.validation.Valid;

/**
 * Self-service profile endpoints. The authenticated principal is the JWT subject
 * (username), so we resolve the User by username before applying changes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp");

    private final UserService userService;

    @Value("${core.upload.avatar-dir}")
    private String avatarDir;

    @Value("${core.upload.avatar-base-url}")
    private String avatarBaseUrl;

    @Value("${core.upload.max-bytes:2097152}")
    private long maxBytes;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponseDTO> me(Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(user));
    }

    @PutMapping
    public ResponseEntity<UserResponseDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request,
            Authentication auth) {
        User user = currentUser(auth);
        User updated = userService.updateProfile(
                user.getUserId(), request.firstName(), request.lastName(), request.phone(), request.bio());
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(updated));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponseDTO> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            Authentication auth) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Avatar file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessValidationException("File too large; max " + maxBytes + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessValidationException("Unsupported image type: " + contentType);
        }

        User user = currentUser(auth);
        String ext = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
        String filename = user.getUserId() + "_" + UUID.randomUUID() + ext;

        Path dir = Paths.get(avatarDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = avatarBaseUrl + "/" + filename;
        User updated = userService.updateAvatarUrl(user.getUserId(), url);
        log.info("Avatar uploaded for user {}: {}", user.getUserId(), url);
        return ResponseEntity.ok(UserMapper.toUserResponseDTO(updated));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessValidationException("Not authenticated");
        }
        String username = String.valueOf(auth.getPrincipal());
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' not found"));
    }
}

package com.mp.core.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.entity.Notification;
import com.mp.core.entity.User;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.service.NotificationService;
import com.mp.core.service.UserService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        String userId = currentUserId(auth);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(notificationService.list(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(Authentication auth) {
        return ResponseEntity.ok(notificationService.unreadCount(currentUserId(auth)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable("id") String id, Authentication auth) {
        notificationService.markRead(currentUserId(auth), id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(currentUserId(auth));
        return ResponseEntity.ok().build();
    }

    private String currentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessValidationException("Not authenticated");
        }
        String username = String.valueOf(auth.getPrincipal());
        User u = userService.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' not found"));
        return u.getUserId();
    }
}

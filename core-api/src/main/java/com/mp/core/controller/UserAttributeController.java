package com.mp.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.SetAttributeRequestDTO;
import com.mp.core.entity.UserAttribute;
import com.mp.core.service.UserAttributeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/{userId}/attributes")
public class UserAttributeController {

    private final UserAttributeService attrService;

    public UserAttributeController(UserAttributeService attrService) {
        this.attrService = attrService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserAttribute>> list(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(attrService.all(userId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserAttribute> set(
            @PathVariable("userId") String userId,
            @Valid @RequestBody SetAttributeRequestDTO request) {
        return ResponseEntity.ok(attrService.set(userId, request.key(), request.value()));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable("userId") String userId,
            @PathVariable("key") String key) {
        attrService.delete(userId, key);
        return ResponseEntity.ok().build();
    }
}

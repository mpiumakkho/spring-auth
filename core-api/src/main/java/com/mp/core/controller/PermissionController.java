package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.CreatePermissionRequestDTO;
import com.mp.core.dto.PermissionIdRequestDTO;
import com.mp.core.dto.PermissionNameRequestDTO;
import com.mp.core.dto.ResourceActionFilterRequestDTO;
import com.mp.core.dto.ResourceFilterRequestDTO;
import com.mp.core.dto.UpdatePermissionRequestDTO;
import com.mp.core.entity.Permission;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.service.PermissionService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'PERMISSION:READ') or hasRole('ADMIN')")
    public ResponseEntity<Page<Permission>> getAllPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity.ok(permissionService.getAllPermissions(pageable));
    }

    @PostMapping("/find-by-id")
    @PreAuthorize("hasPermission(null, 'PERMISSION:READ') or hasRole('ADMIN')")
    public ResponseEntity<Permission> getPermissionById(@Valid @RequestBody PermissionIdRequestDTO request) {
        Permission permission = permissionService.getPermissionById(request.permissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Permission", request.permissionId()));
        return ResponseEntity.ok(permission);
    }

    @PostMapping("/find-by-name")
    @PreAuthorize("hasPermission(null, 'PERMISSION:READ') or hasRole('ADMIN')")
    public ResponseEntity<Permission> getPermissionByName(@Valid @RequestBody PermissionNameRequestDTO request) {
        Permission permission = permissionService.getPermissionByName(request.name())
                .orElseThrow(() -> new ResourceNotFoundException("Permission with name '" + request.name() + "' not found"));
        return ResponseEntity.ok(permission);
    }

    @PostMapping("/find-by-resource")
    @PreAuthorize("hasPermission(null, 'PERMISSION:READ') or hasRole('ADMIN')")
    public ResponseEntity<List<Permission>> getPermissionsByResource(@Valid @RequestBody ResourceFilterRequestDTO request) {
        return ResponseEntity.ok(permissionService.getPermissionsByResource(request.resource()));
    }

    @PostMapping("/find-by-resource-and-action")
    @PreAuthorize("hasPermission(null, 'PERMISSION:READ') or hasRole('ADMIN')")
    public ResponseEntity<List<Permission>> getPermissionsByResourceAndAction(
            @Valid @RequestBody ResourceActionFilterRequestDTO request) {
        return ResponseEntity.ok(
                permissionService.getPermissionsByResourceAndAction(request.resource(), request.action()));
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'PERMISSION:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody CreatePermissionRequestDTO request) {
        Permission permission = new Permission();
        permission.setName(request.name());
        permission.setDescription(request.description());
        Permission created = permissionService.createPermission(permission);
        log.info("Permission created: {} (id={})", created.getName(), created.getPermissionId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'PERMISSION:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody UpdatePermissionRequestDTO request) {
        Permission updates = new Permission();
        updates.setPermissionId(request.permissionId());
        updates.setName(request.name());
        updates.setDescription(request.description());
        Permission updated = permissionService.updatePermission(updates);
        log.info("Permission updated: {}", updated.getPermissionId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'PERMISSION:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<String> deletePermission(@Valid @RequestBody PermissionIdRequestDTO request) {
        if (permissionService.getPermissionById(request.permissionId()).isEmpty()) {
            throw new ResourceNotFoundException("Permission", request.permissionId());
        }
        permissionService.deletePermission(request.permissionId());
        log.info("Permission deleted: {}", request.permissionId());
        return ResponseEntity.ok("Permission deleted successfully with ID: " + request.permissionId());
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}

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

import com.mp.core.dto.AssignPermissionRequestDTO;
import com.mp.core.dto.RoleIdRequestDTO;
import com.mp.core.dto.RoleNameRequestDTO;
import com.mp.core.entity.Permission;
import com.mp.core.entity.Role;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.service.RoleService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'ROLE:READ') or hasRole('ADMIN')")
    public ResponseEntity<Page<Role>> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity.ok(roleService.getAllRoles(pageable));
    }

    @PostMapping("/find-by-id")
    public ResponseEntity<Role> getRoleById(@Valid @RequestBody RoleIdRequestDTO request) {
        Role role = roleService.getRoleById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", request.roleId()));
        return ResponseEntity.ok(role);
    }

    @PostMapping("/find-by-name")
    public ResponseEntity<Role> getRoleByName(@Valid @RequestBody RoleNameRequestDTO request) {
        Role role = roleService.getRoleByName(request.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role with name '" + request.name() + "' not found"));
        return ResponseEntity.ok(role);
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'ROLE:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        if (role.getName() == null || role.getName().isBlank()) {
            throw new BusinessValidationException("Role name is required");
        }
        Role created = roleService.createRole(role);
        log.info("Role created: {} (id={})", created.getName(), created.getRoleId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'ROLE:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<Role> updateRole(@RequestBody Role role) {
        if (role.getRoleId() == null || role.getRoleId().isBlank()) {
            throw new BusinessValidationException("Role ID is required");
        }
        Role updated = roleService.updateRole(role);
        log.info("Role updated: {}", updated.getRoleId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'ROLE:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<String> deleteRole(@Valid @RequestBody RoleIdRequestDTO request) {
        if (roleService.getRoleById(request.roleId()).isEmpty()) {
            throw new ResourceNotFoundException("Role", request.roleId());
        }
        roleService.deleteRole(request.roleId());
        log.info("Role deleted: {}", request.roleId());
        return ResponseEntity.ok("Role deleted successfully with ID: " + request.roleId());
    }

    @PostMapping("/assign-permission")
    public ResponseEntity<String> assignPermissionToRole(@Valid @RequestBody AssignPermissionRequestDTO request) {
        roleService.assignPermissionToRole(request.roleId(), request.permissionId());
        return ResponseEntity.ok("Permission assigned successfully to role ID: " + request.roleId());
    }

    @PostMapping("/remove-permission")
    public ResponseEntity<String> removePermissionFromRole(@Valid @RequestBody AssignPermissionRequestDTO request) {
        roleService.removePermissionFromRole(request.roleId(), request.permissionId());
        return ResponseEntity.ok("Permission removed successfully from role ID: " + request.roleId());
    }

    @PostMapping("/get-permissions")
    public ResponseEntity<List<Permission>> getRolePermissions(@Valid @RequestBody RoleIdRequestDTO request) {
        return ResponseEntity.ok(roleService.getRolePermissions(request.roleId()));
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}

package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.entity.Permission;
import com.mp.core.service.PermissionService;

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
    public ResponseEntity<?> getAllPermissions(
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
            return ResponseEntity.ok(permissionService.getAllPermissions(pageable));
        } catch (Exception e) {
            log.error("Error retrieving permissions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error retrieving permissions");
        }
    }

    @PostMapping("/find-by-id")
    public ResponseEntity<?> getPermissionById(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String permissionId = json.optString("permissionId");
            
            // log.info("Finding permission by ID: {}", permissionId);
            
            if (permissionId == null || permissionId.trim().isEmpty()) {
                // log.warn("Permission ID is required but not provided");
                return ResponseEntity.badRequest().body("Permission ID required");
            }

            Optional<Permission> permission = permissionService.getPermissionById(permissionId);
            if (permission.isPresent()) {
                // log.info("Permission found: {}", permissionId);
                return ResponseEntity.ok(permission.get());
            } else {
                // log.warn("Permission not found: {}", permissionId);
                return ResponseEntity.status(404).body("Permission not found with ID: " + permissionId);
            }
        } catch (Exception e) {
            // log.error("Error finding permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding permission: " + e.getMessage());
        }
    }

    @PostMapping("/find-by-name")
    public ResponseEntity<?> getPermissionByName(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String name = json.optString("name");
            
            // log.info("Finding permission by name: {}", name);
            
            if (name == null || name.trim().isEmpty()) {
                // log.warn("Permission name is required but not provided");
                return ResponseEntity.badRequest().body("Permission name required");
            }

            Optional<Permission> permission = permissionService.getPermissionByName(name);
            if (permission.isPresent()) {
                // log.info("Permission found by name: {}", name);
                return ResponseEntity.ok(permission.get());
            } else {
                // log.warn("Permission not found by name: {}", name);
                return ResponseEntity.status(404).body("Permission not found with name: " + name);
            }
        } catch (Exception e) {
            // log.error("Error finding permission by name: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding permission by name: " + e.getMessage());
        }
    }

    @PostMapping("/find-by-resource")
    public ResponseEntity<?> getPermissionsByResource(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String resource = json.optString("resource");
            
            // log.info("Finding permissions by resource: {}", resource);
            
            if (resource == null || resource.trim().isEmpty()) {
                // log.warn("Resource is required but not provided");
                return ResponseEntity.badRequest().body("Resource required");
            }

            List<Permission> permissions = permissionService.getPermissionsByResource(resource);
            // log.info("Found {} permissions for resource: {}", permissions.size(), resource);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            // log.error("Error finding permissions by resource: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding permissions by resource: " + e.getMessage());
        }
    }

    @PostMapping("/find-by-resource-and-action")
    public ResponseEntity<?> getPermissionsByResourceAndAction(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String resource = json.optString("resource");
            String action = json.optString("action");
            
            // log.info("Finding permissions by resource: {} and action: {}", resource, action);
            
            if (resource == null || resource.trim().isEmpty() || action == null || action.trim().isEmpty()) {
                // log.warn("Resource and action are required but not provided");
                return ResponseEntity.badRequest().body("Resource and action required");
            }

            List<Permission> permissions = permissionService.getPermissionsByResourceAndAction(resource, action);
            // log.info("Found {} permissions for resource: {} and action: {}", permissions.size(), resource, action);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            // log.error("Error finding permissions by resource and action: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding permissions by resource and action: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'PERMISSION:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<?> createPermission(@RequestBody Permission permission) {
        log.info("Creating new permission: {}", permission.getName());
        
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            // log.warn("Permission name is required but not provided");
            return ResponseEntity.badRequest().body("Permission name required");
        }
        if (permission.getResource() == null || permission.getResource().trim().isEmpty()) {
            // log.warn("Permission resource is required but not provided");
            return ResponseEntity.badRequest().body("Permission resource required");
        }
        if (permission.getAction() == null || permission.getAction().trim().isEmpty()) {
            // log.warn("Permission action is required but not provided");
            return ResponseEntity.badRequest().body("Permission action required");
        }

        try {
            Permission createdPermission = permissionService.createPermission(permission);
            // log.info("Permission created successfully: {} with ID: {}", permission.getName(), createdPermission.getPermissionId());
            return ResponseEntity.ok(createdPermission);
        } catch (IllegalArgumentException e) {
            // log.warn("Permission creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Permission creation failed: " + e.getMessage());
        } catch (Exception e) {
            // log.error("Error creating permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error creating permission: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'PERMISSION:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<?> updatePermission(@RequestBody Permission permission) {
        log.info("Updating permission: {}", permission.getPermissionId());
        
        if (permission.getPermissionId() == null || permission.getPermissionId().trim().isEmpty()) {
            // log.warn("Permission ID is required but not provided");
            return ResponseEntity.badRequest().body("Permission ID required");
        }

        try {
            Permission updatedPermission = permissionService.updatePermission(permission);
            // log.info("Permission updated successfully: {}", permission.getPermissionId());
            return ResponseEntity.ok(updatedPermission);
        } catch (RuntimeException e) {
            // log.warn("Permission update failed: {}", e.getMessage());
            if (e.getMessage().contains("Permission not found")) {
                return ResponseEntity.status(404).body("Permission not found with ID: " + permission.getPermissionId());
            } else {
                return ResponseEntity.badRequest().body("Permission update failed: " + e.getMessage());
            }
        } catch (Exception e) {
            // log.error("Error updating permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error updating permission: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'PERMISSION:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<?> deletePermission(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String permissionId = json.optString("permissionId");
            
            // log.info("Deleting permission: {}", permissionId);
            
            if (permissionId == null || permissionId.trim().isEmpty()) {
                // log.warn("Permission ID is required but not provided");
                return ResponseEntity.badRequest().body("Permission ID required");
            }

            // check permission before delete
            Optional<Permission> permission = permissionService.getPermissionById(permissionId);
            if (!permission.isPresent()) {
                // log.warn("Permission not found for deletion: {}", permissionId);
                return ResponseEntity.status(404).body("Permission not found with ID: " + permissionId);
            }
            
            permissionService.deletePermission(permissionId);
            // log.info("Permission deleted successfully: {}", permissionId);
            return ResponseEntity.ok("Permission deleted successfully with ID: " + permissionId);
        } catch (Exception e) {
            // log.error("Error deleting permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error deleting permission: " + e.getMessage());
        }
    }
} 
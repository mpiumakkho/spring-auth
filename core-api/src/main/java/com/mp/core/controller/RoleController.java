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
import com.mp.core.entity.Role;
import com.mp.core.service.RoleService;

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
    public ResponseEntity<?> getAllRoles(
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
            return ResponseEntity.ok(roleService.getAllRoles(pageable));
        } catch (Exception e) {
            log.error("Error retrieving roles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error retrieving roles");
        }
    }

    @PostMapping("/find-by-id")
    public ResponseEntity<?> getRoleById(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String roleId = json.optString("roleId");
            
            log.info("Finding role by ID: {}", roleId);
            
            if (roleId == null || roleId.trim().isEmpty()) {
                log.warn("Role ID is required but not provided");
                return ResponseEntity.badRequest().body("Role ID required");
            }

            Optional<Role> role = roleService.getRoleById(roleId);
            if (role.isPresent()) {
                log.info("Role found: {}", roleId);
                return ResponseEntity.ok(role.get());
            } else {
                log.warn("Role not found: {}", roleId);
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            }
        } catch (Exception e) {
            log.error("Error finding role: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding role: " + e.getMessage());
        }
    }

    @PostMapping("/find-by-name")
    public ResponseEntity<?> getRoleByName(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String name = json.optString("name");
            
            log.info("Finding role by name: {}", name);
            
            if (name == null || name.trim().isEmpty()) {
                log.warn("Role name is required but not provided");
                return ResponseEntity.badRequest().body("Role name required");
            }

            Optional<Role> role = roleService.getRoleByName(name);
            if (role.isPresent()) {
                log.info("Role found by name: {}", name);
                return ResponseEntity.ok(role.get());
            } else {
                log.warn("Role not found by name: {}", name);
                return ResponseEntity.status(404).body("Role not found with name: " + name);
            }
        } catch (Exception e) {
            log.error("Error finding role by name: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error finding role by name: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasPermission(null, 'ROLE:CREATE') or hasRole('ADMIN')")
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        log.info("Creating new role: {}", role.getName());
        
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            log.warn("Role name is required but not provided");
            return ResponseEntity.badRequest().body("Role name required");
        }

        try {
            Role createdRole = roleService.createRole(role);
            log.info("Role created successfully: {} with ID: {}", role.getName(), createdRole.getRoleId());
            return ResponseEntity.ok(createdRole);
        } catch (IllegalArgumentException e) {
            log.warn("Role creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Role creation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating role: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error creating role: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(null, 'ROLE:UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<?> updateRole(@RequestBody Role role) {
        log.info("Updating role: {}", role.getRoleId());
        
        if (role.getRoleId() == null || role.getRoleId().trim().isEmpty()) {
            log.warn("Role ID is required but not provided");
            return ResponseEntity.badRequest().body("Role ID required");
        }

        try {
            Role updatedRole = roleService.updateRole(role);
            log.info("Role updated successfully: {}", role.getRoleId());
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) {
            log.warn("Role update failed: {}", e.getMessage());
            if (e.getMessage().contains("Role not found")) {
                return ResponseEntity.status(404).body("Role not found with ID: " + role.getRoleId());
            } else {
                return ResponseEntity.badRequest().body("Role update failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error updating role: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error updating role: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(null, 'ROLE:DELETE') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteRole(@RequestBody String request) {
        try {
            JSONObject json = new JSONObject(request);
            String roleId = json.optString("roleId");
            
            log.info("Deleting role: {}", roleId);
            
            if (roleId == null || roleId.trim().isEmpty()) {
                log.warn("Role ID is required but not provided");
                return ResponseEntity.badRequest().body("Role ID required");
            }

            // check role before delete
            Optional<Role> role = roleService.getRoleById(roleId);
            if (!role.isPresent()) {
                log.warn("Role not found for deletion: {}", roleId);
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            }
            
            roleService.deleteRole(roleId);
            log.info("Role deleted successfully: {}", roleId);
            return ResponseEntity.ok("Role deleted successfully with ID: " + roleId);
        } catch (Exception e) {
            log.error("Error deleting role: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error deleting role: " + e.getMessage());
        }
    }

    @PostMapping("/assign-permission")
    public ResponseEntity<?> assignPermissionToRole(@RequestBody String request) {
        String roleId = null;
        String permissionId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            roleId = json.optString("roleId");
            permissionId = json.optString("permissionId");
            
            log.info("Assigning permission {} to role {}", permissionId, roleId);
            
            if (roleId == null || roleId.trim().isEmpty() || permissionId == null || permissionId.trim().isEmpty()) {
                log.warn("Role ID and Permission ID are required but not provided");
                return ResponseEntity.badRequest().body("Role ID and Permission ID required");
            }

            roleService.assignPermissionToRole(roleId, permissionId);
            log.info("Permission {} assigned successfully to role {}", permissionId, roleId);
            return ResponseEntity.ok("Permission assigned successfully to role ID: " + roleId);
        } catch (RuntimeException e) {
            log.warn("Permission assignment failed: {}", e.getMessage());
            if (e.getMessage().contains("Role not found")) {
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            } else if (e.getMessage().contains("Permission not found")) {
                return ResponseEntity.status(404).body("Permission not found");
            } else {
                return ResponseEntity.badRequest().body("Permission assignment failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error assigning permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error assigning permission: " + e.getMessage());
        }
    }

    @PostMapping("/remove-permission")
    public ResponseEntity<?> removePermissionFromRole(@RequestBody String request) {
        String roleId = null;
        String permissionId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            roleId = json.optString("roleId");
            permissionId = json.optString("permissionId");
            
            log.info("Removing permission {} from role {}", permissionId, roleId);
            
            if (roleId == null || roleId.trim().isEmpty() || permissionId == null || permissionId.trim().isEmpty()) {
                log.warn("Role ID and Permission ID are required but not provided");
                return ResponseEntity.badRequest().body("Role ID and Permission ID required");
            }

            roleService.removePermissionFromRole(roleId, permissionId);
            log.info("Permission {} removed successfully from role {}", permissionId, roleId);
            return ResponseEntity.ok("Permission removed successfully from role ID: " + roleId);
        } catch (RuntimeException e) {
            log.warn("Permission removal failed: {}", e.getMessage());
            if (e.getMessage().contains("Role not found")) {
                return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
            } else if (e.getMessage().contains("Permission not found")) {
                return ResponseEntity.status(404).body("Permission not found");
            } else {
                return ResponseEntity.badRequest().body("Permission removal failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error removing permission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error removing permission: " + e.getMessage());
        }
    }

    @PostMapping("/get-permissions")
    public ResponseEntity<?> getRolePermissions(@RequestBody String request) {
        String roleId = null;
        
        try {
            JSONObject json = new JSONObject(request);
            roleId = json.optString("roleId");
            
            log.info("Getting permissions for role: {}", roleId);
            
            if (roleId == null || roleId.trim().isEmpty()) {
                log.warn("Role ID is required but not provided");
                return ResponseEntity.badRequest().body("Role ID required");
            }

            List<Permission> permissions = roleService.getRolePermissions(roleId);
            log.info("Found {} permissions for role: {}", permissions.size(), roleId);
            return ResponseEntity.ok(permissions);
        } catch (RuntimeException e) {
            log.warn("Get permissions failed: {}", e.getMessage());
            return ResponseEntity.status(404).body("Role not found with ID: " + roleId);
        } catch (Exception e) {
            log.error("Error getting role permissions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error getting role permissions: " + e.getMessage());
        }
    }
} 
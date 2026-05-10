package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Permission;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.DuplicateResourceException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.PermissionRepository;
import com.mp.core.service.AuditService;
import com.mp.core.service.PermissionService;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {    
    // Pattern for valid permission format: resource:action
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[a-zA-Z_]+:[a-zA-Z_]+$");
    
    private final PermissionRepository permRepo;
    private final AuditService auditService;

    public PermissionServiceImpl(PermissionRepository permRepo, AuditService auditService) {
        this.permRepo = permRepo;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public Permission createPermission(Permission permission) {
        // Basic validation
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            log.warn("Attempt to create permission with empty name");
            throw new BusinessValidationException("Permission name is required");
        }

        // Format validation
        String permName = permission.getName().toLowerCase();
        if (!PERMISSION_PATTERN.matcher(permName).matches()) {
            log.warn("Invalid permission format: {}", permName);
            throw new BusinessValidationException(
                "Permission must be in format 'resource:action' using only letters and underscores"
            );
        }

        // Check for duplicates
        if (permRepo.existsByName(permName)) {
            log.warn("Permission already exists: {}", permName);
            throw new DuplicateResourceException("Permission", "name", permName);
        }

        // Parse and set resource/action
        String[] parts = permName.split(":");
        permission.setResource(parts[0]);
        permission.setAction(parts[1]);
        permission.setName(permName); // Store normalized name

        log.info("Creating new permission: {}", permName);
        Permission saved = permRepo.save(permission);
        auditService.log(permission.getCreatedBy(), "CREATE", "PERMISSION", saved.getPermissionId(), "Created permission: " + permName);
        return saved;
    }

    @Override
    @Transactional
    public Permission updatePermission(Permission permission) {
        // Find existing permission
        Permission existing = permRepo.findById(permission.getPermissionId())
            .orElseThrow(() -> {
                log.warn("Attempt to update non-existent permission: {}", permission.getPermissionId());
                return new ResourceNotFoundException("Permission", permission.getPermissionId());
            });

        String newName = permission.getName().toLowerCase();
        
        // If name is changing, validate new name
        if (!existing.getName().equals(newName)) {
            if (!PERMISSION_PATTERN.matcher(newName).matches()) {
                throw new BusinessValidationException(
                    "Permission must be in format 'resource:action' using only letters and underscores"
                );
            }
            
            if (permRepo.existsByName(newName)) {
                log.warn("Permission name conflict: {}", newName);
                throw new DuplicateResourceException("Permission", "name", newName);
            }

            String[] parts = newName.split(":");
            existing.setResource(parts[0]);
            existing.setAction(parts[1]);
            existing.setName(newName);
        }

        // Update other fields
        if (permission.getDescription() != null) {
            existing.setDescription(permission.getDescription());
        }
        
        existing.setUpdatedBy(permission.getUpdatedBy());

        log.info("Updating permission: {}", existing.getPermissionId());
        Permission updated = permRepo.save(existing);
        auditService.log(permission.getUpdatedBy(), "UPDATE", "PERMISSION", updated.getPermissionId(), "Updated permission: " + updated.getName());
        return updated;
    }

    @Override
    @Transactional
    public void deletePermission(String id) {
        if (!permRepo.existsById(id)) {
            log.warn("Attempt to delete non-existent permission: {}", id);
            throw new ResourceNotFoundException("Permission", id);
        }

        // Check if this is a system permission
        Optional<Permission> perm = permRepo.findById(id);
        if (perm.isPresent() && isSystemPermission(perm.get())) {
            log.error("Attempt to delete system permission: {}", perm.get().getName());
            throw new BusinessValidationException("Cannot delete system permission");
        }

        permRepo.deleteById(id);
        auditService.log(null, "DELETE", "PERMISSION", id, "Deleted permission");
        log.info("Permission deleted: {}", id);
    }

    private boolean isSystemPermission(Permission permission) {
        String name = permission.getName().toLowerCase();
        return name.startsWith("system:") || 
               name.contains(":admin") || 
               name.equals("user:create") ||
               name.equals("user:read");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(String id) {
        return permRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByName(String name) {
        return permRepo.findByName(name.toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Permission> getAllPermissions(Pageable pageable) {
        return permRepo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        if (resource == null || resource.trim().isEmpty()) {
            log.warn("Attempt to search permissions with empty resource");
            throw new BusinessValidationException("Resource name is required");
        }
        return permRepo.findByResource(resource.toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResourceAndAction(String resource, String action) {
        if (resource == null || resource.trim().isEmpty()) {
            throw new BusinessValidationException("Resource name is required");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new BusinessValidationException("Action name is required");
        }
        
        return permRepo.findByResourceAndAction(
            resource.toLowerCase(), 
            action.toLowerCase()
        );
    }
} 
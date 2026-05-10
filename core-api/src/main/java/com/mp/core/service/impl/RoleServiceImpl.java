package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Permission;
import com.mp.core.entity.Role;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.DuplicateResourceException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.PermissionRepository;
import com.mp.core.repository.RoleRepository;
import com.mp.core.service.AuditService;
import com.mp.core.service.RoleService;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {    
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final AuditService auditService;

    public RoleServiceImpl(RoleRepository roleRepo, PermissionRepository permRepo, AuditService auditService) {
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        String roleName = role.getName();
        
        if (roleName == null || roleName.trim().isEmpty()) {
            log.warn("Attempt to create role with empty name");
            throw new BusinessValidationException("Role name cannot be empty");
        }
        
        if (roleRepo.existsByName(roleName)) {
            log.warn("Role name '{}' already exists", roleName);
            throw new DuplicateResourceException("Role", "name", roleName);
        }

        role.setName(roleName.toUpperCase());
        
        log.info("Creating new role: {}", roleName);
        Role saved = roleRepo.save(role);
        auditService.log(role.getCreatedBy(), "CREATE", "ROLE", saved.getRoleId(), "Created role: " + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public Role updateRole(Role role) {
        Role existing = roleRepo.findById(role.getRoleId())
            .orElseThrow(() -> {
                log.warn("Attempt to update non-existent role: {}", role.getRoleId());
                return new ResourceNotFoundException("Role", role.getRoleId());
            });
        
        String newName = role.getName();
        
        if (!existing.getName().equals(newName)) {
            if (roleRepo.existsByName(newName)) {
                log.warn("Role name '{}' is already taken", newName);
                throw new DuplicateResourceException("Role", "name", newName);
            }
            existing.setName(newName.toUpperCase());
        }
        
        existing.setDescription(role.getDescription());
        existing.setUpdatedBy(role.getUpdatedBy());

        log.info("Updating role: {}", existing.getRoleId());
        Role updated = roleRepo.save(existing);
        auditService.log(role.getUpdatedBy(), "UPDATE", "ROLE", updated.getRoleId(), "Updated role: " + updated.getName());
        return updated;
    }

    @Override
    @Transactional
    public void deleteRole(String id) {
        if (!roleRepo.existsById(id)) {
            log.warn("Attempt to delete non-existent role: {}", id);
            throw new ResourceNotFoundException("Role", id);
        }
        
        Optional<Role> role = roleRepo.findById(id);
        if (role.isPresent() && isSystemRole(role.get().getName())) {
            log.error("Attempt to delete system role: {}", role.get().getName());
            throw new BusinessValidationException("Cannot delete system role");
        }
        
        roleRepo.deleteById(id);
        auditService.log(null, "DELETE", "ROLE", id, "Deleted role");
        log.info("Role deleted successfully: {}", id);
    }

    private boolean isSystemRole(String roleName) {
        Set<String> systemRoles = Set.of("ADMIN", "USER", "SYSTEM");
        return systemRoles.contains(roleName.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(String id) {
        return roleRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        return roleRepo.findByName(name.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Role> getAllRoles(Pageable pageable) {
        return roleRepo.findAll(pageable);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId")
    public void assignPermissionToRole(String roleId, String permissionId) {
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> {
                log.warn("Role not found: {}", roleId);
                return new ResourceNotFoundException("Role", roleId);
            });

        Permission permission = permRepo.findById(permissionId)
            .orElseThrow(() -> {
                log.warn("Permission not found: {}", permissionId);
                return new ResourceNotFoundException("Permission", permissionId);
            });

        if (role.getPermissions().contains(permission)) {
            log.debug("Permission {} is already assigned to role {}", permissionId, roleId);
            return;
        }

        role.getPermissions().add(permission);
        roleRepo.save(role);
        log.info("Permission {} assigned to role {}", permissionId, roleId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId")
    public void removePermissionFromRole(String roleId, String permissionId) {
        Role role = roleRepo.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
            
        Permission permission = permRepo.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));

        if (isSystemRole(role.getName()) && isCriticalPermission(permission)) {
            log.warn("Attempt to remove critical permission from system role");
            throw new BusinessValidationException("Cannot remove critical permission from system role");
        }

        if (role.getPermissions().remove(permission)) {
            roleRepo.save(role);
            log.info("Permission {} removed from role {}", permissionId, roleId);
        } else {
            log.debug("Permission {} was not assigned to role {}", permissionId, roleId);
        }
    }

    private boolean isCriticalPermission(Permission permission) {
        return permission.getName().toUpperCase().contains("ADMIN") ||
               permission.getName().toUpperCase().contains("SYSTEM");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "rolePermissions", key = "#roleId")
    public List<Permission> getRolePermissions(String roleId) {
        return roleRepo.findById(roleId)
            .map(role -> role.getPermissions().stream()
                .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                .collect(Collectors.toList()))
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }
}

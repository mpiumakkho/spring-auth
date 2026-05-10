package com.mp.core.security;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Custom Permission Evaluator for fine-grained authorization.
 * Evaluates permissions in format: RESOURCE:ACTION (e.g., USER:READ, ROLE:CREATE)
 * 
 * Used with @PreAuthorize("hasPermission(null, 'USER:READ')")
 * 
 * How it works:
 * 1. Receives permission string from @PreAuthorize annotation (e.g., "USER:READ")
 * 2. Converts to authority format: PERM_USER:READ
 * 3. Checks if user has this authority in their granted authorities
 * 
 * Example:
 * - @PreAuthorize("hasPermission(null, 'USER:READ')")
 *   → Checks for authority: PERM_USER:READ
 * 
 * - @PreAuthorize("hasPermission(null, 'ROLE:CREATE')")
 *   → Checks for authority: PERM_ROLE:CREATE
 */
@Slf4j
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {    private static final String PERMISSION_PREFIX = "PERM_";

    /**
     * Evaluate permission for domain object.
     * In our implementation, we don't use targetDomainObject (pass null).
     * 
     * @param authentication Current user's authentication
     * @param targetDomainObject Domain object (not used, pass null)
     * @param permission Permission string in format "RESOURCE:ACTION"
     * @return true if user has the permission
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission
    ) {
        if (authentication == null || permission == null) {
            log.debug("Authentication or permission is null");
            return false;
        }

        // Check if user is authenticated
        if (!authentication.isAuthenticated()) {
            log.debug("User is not authenticated");
            return false;
        }

        // Convert permission to string
        String permissionString = permission.toString().trim().toUpperCase();
        
        // Validate permission format (RESOURCE:ACTION)
        if (!isValidPermissionFormat(permissionString)) {
            log.warn("Invalid permission format: {}. Expected format: RESOURCE:ACTION", permissionString);
            return false;
        }

        // Build authority string with prefix
        String requiredAuthority = PERMISSION_PREFIX + permissionString;

        // Check if user has the required authority
        boolean hasPermission = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals(requiredAuthority));

        if (hasPermission) {
            log.debug("Permission granted: user={}, permission={}", 
                authentication.getName(), permissionString);
        } else {
            log.debug("Permission denied: user={}, permission={}, authorities={}", 
                authentication.getName(), permissionString, 
                authentication.getAuthorities().size());
        }

        return hasPermission;
    }

    /**
     * Evaluate permission by target ID and type.
     * This overload is for resource-specific permissions.
     * 
     * Example usage:
     * @PreAuthorize("hasPermission(#userId, 'User', 'UPDATE')")
     * 
     * @param authentication Current user's authentication
     * @param targetId ID of the target resource
     * @param targetType Type of the target resource
     * @param permission Permission action (READ, CREATE, UPDATE, DELETE)
     * @return true if user has the permission
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {
        if (authentication == null || targetType == null || permission == null) {
            log.debug("Authentication, targetType, or permission is null");
            return false;
        }

        if (!authentication.isAuthenticated()) {
            log.debug("User is not authenticated");
            return false;
        }

        // Build permission string: RESOURCE:ACTION
        String permissionString = String.format("%s:%s", 
            targetType.trim().toUpperCase(),
            permission.toString().trim().toUpperCase()
        );

        // Validate format
        if (!isValidPermissionFormat(permissionString)) {
            log.warn("Invalid permission format: {}. Expected format: RESOURCE:ACTION", permissionString);
            return false;
        }

        // Build authority string with prefix
        String requiredAuthority = PERMISSION_PREFIX + permissionString;

        // Check if user has the required authority
        boolean hasPermission = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals(requiredAuthority));

        if (hasPermission) {
            log.debug("Permission granted: user={}, targetId={}, permission={}", 
                authentication.getName(), targetId, permissionString);
        } else {
            log.debug("Permission denied: user={}, targetId={}, permission={}", 
                authentication.getName(), targetId, permissionString);
        }

        return hasPermission;
    }

    /**
     * Validate permission format.
     * Expected: RESOURCE:ACTION (e.g., USER:READ, ROLE:CREATE)
     * 
     * @param permission Permission string to validate
     * @return true if format is valid
     */
    private boolean isValidPermissionFormat(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        // Must contain exactly one colon
        int colonIndex = permission.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= permission.length() - 1) {
            return false;
        }

        // Check no additional colons
        if (permission.indexOf(':', colonIndex + 1) != -1) {
            return false;
        }

        // Resource and action must not be empty
        String resource = permission.substring(0, colonIndex).trim();
        String action = permission.substring(colonIndex + 1).trim();

        return !resource.isEmpty() && !action.isEmpty();
    }

    /**
     * Check if user has any of the specified permissions.
     * Helper method for complex authorization scenarios.
     * 
     * @param authentication Current user's authentication
     * @param permissions Array of permissions to check
     * @return true if user has at least one permission
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        if (authentication == null || permissions == null || permissions.length == 0) {
            return false;
        }

        for (String permission : permissions) {
            if (hasPermission(authentication, null, permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user has all specified permissions.
     * Helper method for complex authorization scenarios.
     * 
     * @param authentication Current user's authentication
     * @param permissions Array of permissions to check
     * @return true if user has all permissions
     */
    public boolean hasAllPermissions(Authentication authentication, String... permissions) {
        if (authentication == null || permissions == null || permissions.length == 0) {
            return false;
        }

        for (String permission : permissions) {
            if (!hasPermission(authentication, null, permission)) {
                return false;
            }
        }

        return true;
    }
}

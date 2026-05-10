package com.mp.core.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.mp.core.entity.User;
import com.mp.core.service.UserAttributeService;
import com.mp.core.service.UserService;

/**
 * SpEL-callable ABAC policy helpers usable from @PreAuthorize:
 *
 *   @PreAuthorize("hasPermission(null, 'USER:READ') and @attributePolicy.sameAttribute(#userId, 'department')")
 *   @PreAuthorize("@attributePolicy.attributeEquals('level', 'manager')")
 *
 * The current authenticated user's identity comes from the Spring SecurityContext
 * (populated by TokenFilter from the JWT subject — username).
 */
@Component("attributePolicy")
public class AttributePolicy {

    private final UserService userService;
    private final UserAttributeService attrService;

    public AttributePolicy(UserService userService, UserAttributeService attrService) {
        this.userService = userService;
        this.attrService = attrService;
    }

    /** True iff the current user's value for `key` equals the target user's value for `key`. */
    public boolean sameAttribute(String targetUserId, String key) {
        String currentUserId = currentUserId();
        if (currentUserId == null || targetUserId == null) return false;
        Optional<String> a = attrService.get(currentUserId, key);
        Optional<String> b = attrService.get(targetUserId, key);
        return a.isPresent() && b.isPresent() && a.get().equalsIgnoreCase(b.get());
    }

    /** Convenience: sameAttribute with the most common key. */
    public boolean sameDepartment(String targetUserId) {
        return sameAttribute(targetUserId, "department");
    }

    /** True iff the current user's attribute `key` equals the literal `expected`. */
    public boolean attributeEquals(String key, String expected) {
        String userId = currentUserId();
        if (userId == null) return false;
        return attrService.get(userId, key)
                .map(v -> v.equalsIgnoreCase(expected))
                .orElse(false);
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String username = String.valueOf(auth.getPrincipal());
        return userService.getUserByUsername(username).map(User::getUserId).orElse(null);
    }
}

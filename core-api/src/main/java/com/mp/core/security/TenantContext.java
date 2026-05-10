package com.mp.core.security;

/**
 * Thread-local holder for the active tenant id. Populated by TokenFilter from the
 * JWT 'tid' claim (or X-Tenant-Id header for unauthenticated public endpoints),
 * read by Hibernate filters and tenant-scoped service code.
 *
 * SUPER_ADMIN authentication tokens may set tenantId to null, meaning "skip filter".
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void setSuperAdmin(boolean superAdmin) {
        SUPER.set(superAdmin);
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER.get());
    }

    public static void clear() {
        CURRENT.remove();
        SUPER.remove();
    }
}

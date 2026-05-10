package com.mp.core.entity;

import java.util.Set;

public final class UserStatus {

    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String PENDING = "pending";
    public static final String LOCKED = "locked";

    public static final Set<String> ALL = Set.of(ACTIVE, INACTIVE, PENDING, LOCKED);

    private UserStatus() {}

    public static boolean isValid(String status) {
        return status != null && ALL.contains(status);
    }
}

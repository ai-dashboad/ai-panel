package com.aipanel.user.core.security;

import java.util.Locale;

/**
 * User roles stored in {@code t_user.role} and issued as the {@code role} claim of access tokens.
 */
public final class UserRole {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private UserRole() {
    }

    /** Normalizes a stored role; anything missing or unknown is treated as {@link #USER}. */
    public static String normalize(String role) {
        if (role == null) {
            return USER;
        }
        String upper = role.trim().toUpperCase(Locale.ROOT);
        return ADMIN.equals(upper) ? ADMIN : USER;
    }

    public static boolean isAdmin(String role) {
        return ADMIN.equals(normalize(role));
    }
}

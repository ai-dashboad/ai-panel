package com.aipanel.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the {@code CORS_ALLOWED_ORIGINS} setting (comma-separated origins) shared by the gateway and
 * services. Entries are trimmed, trailing slashes removed and duplicates dropped. Entries may be exact
 * origins ({@code https://panel.example.com}) or Spring origin patterns ({@code https://*.example.com}).
 * A bare {@code *} is accepted only as the sole entry (explicit opt-in to allow any origin).
 */
public final class CorsOrigins {

    private CorsOrigins() {
    }

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> origins = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String origin = part.trim();
            while (origin.endsWith("/")) {
                origin = origin.substring(0, origin.length() - 1);
            }
            if (origin.isEmpty()) {
                continue;
            }
            if (!"*".equals(origin) && !origin.matches("^https?://[^\\s/]+$")) {
                throw new IllegalArgumentException(
                    "Invalid CORS origin '" + origin + "': expected scheme://host[:port], e.g. https://panel.example.com");
            }
            origins.add(origin);
        }
        if (origins.contains("*") && origins.size() > 1) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS: '*' must be the only entry when used");
        }
        return new ArrayList<>(origins);
    }

    public static boolean allowsAny(List<String> origins) {
        return origins.contains("*");
    }
}

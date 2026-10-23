package in.urlbit.links.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks aliases and generated codes against system reserved routes.
 *
 * <p>Checks are strictly case-insensitive (e.g. 'Login', 'DASHBOARD' are rejected) to prevent short
 * codes from colliding with or impersonating frontend/API routes.
 */
@Component
public class ReservedPathValidator {

    private static final Set<String> RESERVED_WORDS =
            Set.of(
                    "api",
                    "login",
                    "signup",
                    "register",
                    "logout",
                    "dashboard",
                    "settings",
                    "admin",
                    "health",
                    "actuator",
                    "swagger-ui",
                    "v3",
                    "docs",
                    "robots.txt",
                    "favicon.ico",
                    "metrics",
                    "prometheus",
                    "error",
                    "static",
                    "assets",
                    "public",
                    "links",
                    "urls",
                    "me");

    public boolean isReserved(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.strip().toLowerCase(Locale.ROOT);
        return RESERVED_WORDS.contains(normalized);
    }
}

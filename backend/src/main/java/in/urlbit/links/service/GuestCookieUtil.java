package in.urlbit.links.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Handles the issuance, hashing, and response cookie setting for guest identifiers.
 *
 * <p>Guest cookie has a fixed Max-Age of 30 days that is never refreshed, ensuring that guest
 * quotas cannot be artificially extended or reset by continuous activity.
 */
@Component
public class GuestCookieUtil {

    public static final String COOKIE_NAME = "urlbit_guest_id";
    public static final int COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 30 days

    public GuestContext resolveGuest(HttpServletRequest request, HttpServletResponse response) {
        String rawGuestId = null;
        boolean isNew = false;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    rawGuestId = cookie.getValue().strip();
                    break;
                }
            }
        }

        if (rawGuestId == null) {
            rawGuestId = UUID.randomUUID().toString();
            isNew = true;
        }

        if (isNew) {
            Cookie newCookie = new Cookie(COOKIE_NAME, rawGuestId);
            newCookie.setHttpOnly(true);
            newCookie.setPath("/");
            newCookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
            // In a production HTTPS deployment, this would be set to true
            newCookie.setSecure(request.isSecure());
            newCookie.setAttribute("SameSite", "Lax");
            response.addCookie(newCookie);
        }

        String hash = sha256Hex(rawGuestId);
        return new GuestContext(rawGuestId, hash, isNew);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest unavailable", ex);
        }
    }

    public record GuestContext(String rawId, String idHash, boolean isNew) {}
}

package in.urlbit.analytics.service;

import java.net.IDN;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Sanitizes HTTP Referer headers down to normalized domain names.
 *
 * <p>Strips path, query parameters, fragments, userinfo, and ports to prevent sensitive query
 * tokens or user identifiers from leaking into stored analytics tables. Falls back to "direct" when
 * the referrer is absent, malformed, blank, or has no valid hostname.
 */
@Component
public class ReferrerSanitizer {

    public static final String DIRECT = "direct";
    private static final int MAX_LENGTH = 255;

    public String sanitize(String rawReferer) {
        if (rawReferer == null || rawReferer.isBlank()) {
            return DIRECT;
        }

        String trimmed = rawReferer.strip();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        String hostPart;
        if (lower.startsWith("http://")) {
            hostPart = trimmed.substring(7);
        } else if (lower.startsWith("https://")) {
            hostPart = trimmed.substring(8);
        } else if (lower.contains("://") || lower.contains(":")) {
            // Non-HTTP schemes like ftp://, file://, about:blank, javascript:, data:
            return DIRECT;
        } else {
            // Schemeless referrer like "twitter.com/path"
            hostPart = trimmed;
        }

        // Extract host before delimiter ('/', '?', '#', ':')
        int endIdx = hostPart.length();
        for (int i = 0; i < hostPart.length(); i++) {
            char c = hostPart.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ':') {
                endIdx = i;
                break;
            }
        }

        String candidateHost = hostPart.substring(0, endIdx);
        return normalizeHost(candidateHost);
    }

    private String normalizeHost(String rawHost) {
        if (rawHost == null || rawHost.isBlank() || rawHost.contains(" ")) {
            return DIRECT;
        }

        String host = rawHost.strip().toLowerCase(Locale.ROOT);

        // Strip trailing dot if present
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }

        // A valid internet referrer host must have at least one dot separating domain and TLD
        if (host.isBlank() || !host.contains(".")) {
            return DIRECT;
        }

        try {
            String asciiHost = IDN.toASCII(host);
            if (asciiHost.length() > MAX_LENGTH) {
                asciiHost = asciiHost.substring(0, MAX_LENGTH);
            }
            return asciiHost;
        } catch (IllegalArgumentException ex) {
            return DIRECT;
        }
    }
}

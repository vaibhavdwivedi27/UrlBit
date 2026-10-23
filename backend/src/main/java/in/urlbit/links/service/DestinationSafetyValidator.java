package in.urlbit.links.service;

import in.urlbit.common.error.UnsafeDestinationException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates destination URLs against scheme, format, self-loops, and address denylists.
 *
 * <p>Executed outside the database transaction. Normalizes hosts to ASCII before rejection checks
 * to prevent bypasses via casing, trailing dots, or internationalized domain names.
 */
@Component
public class DestinationSafetyValidator {

    private static final String SERVICE_DOMAIN = "urlbit.in";

    private static final Set<String> DENYLISTED_HOSTS =
            Set.of("localhost", "127.0.0.1", "0.0.0.0", "bit.ly", "tinyurl.com", "goo.gl", "t.co");

    public void validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new UnsafeDestinationException("Destination URL must not be blank");
        }

        if (rawUrl.length() > 2048) {
            throw new UnsafeDestinationException("URL length exceeds limit of 2048 characters");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.strip());
        } catch (URISyntaxException ex) {
            throw new UnsafeDestinationException("Malformed destination URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new UnsafeDestinationException("Only http and https URL schemes are supported");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeDestinationException("Destination URL must have a valid host");
        }

        if (uri.getUserInfo() != null) {
            throw new UnsafeDestinationException("URLs with user credentials are not permitted");
        }

        String normalizedHost = normalizeHost(host);

        // Reject self-domain and all subdomains to prevent redirect loops and chain hiding
        if (normalizedHost.equals(SERVICE_DOMAIN)
                || normalizedHost.endsWith("." + SERVICE_DOMAIN)) {
            throw new UnsafeDestinationException(
                    "Links to urlbit.in and its subdomains are not allowed");
        }

        // Check denylist
        if (DENYLISTED_HOSTS.contains(normalizedHost)) {
            throw new UnsafeDestinationException("Destination domain is on the blocklist");
        }

        // Reject IP literals / loopback / private ranges
        checkIpSafety(normalizedHost);
    }

    public String normalizeHost(String rawHost) {
        String trimmed = rawHost.strip().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try {
            return IDN.toASCII(trimmed);
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }

    private void checkIpSafety(String host) {
        // Test if host is an IP literal or loopback name
        if (host.equals("localhost")) {
            throw new UnsafeDestinationException(
                    "Private and loopback addresses are not permitted");
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            // Only inspect if the input was already an IP literal to avoid blocking valid DNS names
            if (host.matches("^[0-9.]+$") || host.contains(":")) {
                if (addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isAnyLocalAddress()) {
                    throw new UnsafeDestinationException(
                            "Private and loopback addresses are not permitted");
                }
            }
        } catch (UnknownHostException ignored) {
            // Unresolvable domain name is checked separately by Safe Browsing in later milestones
        }
    }
}

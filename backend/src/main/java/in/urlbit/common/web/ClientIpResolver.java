package in.urlbit.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP address from the HTTP servlet request.
 *
 * <p>When behind a trusted reverse proxy ({@code urlbit.security.trust-proxy=true}), inspects the
 * {@code X-Forwarded-For} header, selecting the client address (leftmost entry in the proxy chain).
 * Otherwise, falls back strictly to the direct socket address {@link
 * HttpServletRequest#getRemoteAddr()}.
 */
@Component
public class ClientIpResolver {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ClientIpResolver.class);

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${urlbit.security.trust-proxy:true}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String resolved = null;
        if (trustProxy) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstIp = forwardedFor.split(",")[0].trim();
                if (!firstIp.isEmpty()) {
                    resolved = firstIp;
                }
            }
        }

        if (resolved == null) {
            String remoteAddr = request.getRemoteAddr();
            resolved = remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr.trim() : "unknown";
        }

        log.debug(
                "resolveClientIp -> resolved: '{}', X-Forwarded-For: '{}', getRemoteAddr: '{}'",
                resolved,
                request.getHeader("X-Forwarded-For"),
                request.getRemoteAddr());
        return resolved;
    }
}

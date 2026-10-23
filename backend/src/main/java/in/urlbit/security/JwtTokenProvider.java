package in.urlbit.security;

import in.urlbit.auth.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates and validates short-lived access JWT tokens.
 *
 * <p>Validates algorithm, signature, issuer, expiry, and subject. Default access token lifetime is
 * 15 minutes (900 seconds).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationSeconds;
    private final String issuer;

    public JwtTokenProvider(
            @Value(
                            "${urlbit.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
                    String secret,
            @Value("${urlbit.jwt.access-token-expiration-seconds:900}") long expirationSeconds,
            @Value("${urlbit.jwt.issuer:urlbit.in}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(UUID userId, String email, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public UserPrincipal parseAndValidate(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(key)
                            .requireIssuer(issuer)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String roleStr = claims.get("role", String.class);
            UserRole role = UserRole.valueOf(roleStr);

            return new UserPrincipal(userId, email, role);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid or expired JWT token: {}", ex.getMessage());
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}

package in.urlbit.security;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.auth.domain.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 900, "urlbit.in");
    }

    @Test
    void generatesAndParsesValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "developer@urlbit.in";
        UserRole role = UserRole.USER;

        String token = tokenProvider.generateAccessToken(userId, email, role);
        assertThat(token).isNotBlank();

        UserPrincipal principal = tokenProvider.parseAndValidate(token);
        assertThat(principal).isNotNull();
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo(email);
        assertThat(principal.role()).isEqualTo(role);
    }

    @Test
    void rejectsTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateAccessToken(userId, "test@urlbit.in", UserRole.USER);
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        UserPrincipal principal = tokenProvider.parseAndValidate(tampered);
        assertThat(principal).isNull();
    }

    @Test
    void rejectsExpiredToken() {
        // Provider with -1 second expiration
        var expiredProvider = new JwtTokenProvider(SECRET, -1, "urlbit.in");
        String expiredToken =
                expiredProvider.generateAccessToken(
                        UUID.randomUUID(), "expired@urlbit.in", UserRole.USER);

        UserPrincipal principal = tokenProvider.parseAndValidate(expiredToken);
        assertThat(principal).isNull();
    }

    @Test
    void rejectsTokenWithMismatchedIssuer() {
        var wrongIssuerProvider = new JwtTokenProvider(SECRET, 900, "attacker.com");
        String token =
                wrongIssuerProvider.generateAccessToken(
                        UUID.randomUUID(), "fake@attacker.com", UserRole.USER);

        UserPrincipal principal = tokenProvider.parseAndValidate(token);
        assertThat(principal).isNull();
    }
}

package in.urlbit.auth.service;

import in.urlbit.auth.domain.RefreshToken;
import in.urlbit.auth.domain.User;
import in.urlbit.auth.domain.UserRole;
import in.urlbit.auth.domain.UserStatus;
import in.urlbit.auth.dto.AuthResponse;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.dto.RefreshTokenRequest;
import in.urlbit.auth.dto.SignupRequest;
import in.urlbit.auth.dto.UserResponse;
import in.urlbit.auth.repository.RefreshTokenRepository;
import in.urlbit.auth.repository.UserRepository;
import in.urlbit.common.error.DuplicateEmailException;
import in.urlbit.common.error.ResourceNotFoundException;
import in.urlbit.common.error.UnauthorizedException;
import in.urlbit.links.service.GuestCookieUtil;
import in.urlbit.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshTokenExpirationSeconds;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${urlbit.jwt.refresh-token-expiration-seconds:2592000}")
                    long refreshTokenExpirationSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Instant now = Instant.now();
        // Role is unconditionally USER on signup. Client-supplied role inputs are ignored/rejected.
        User user =
                new User(
                        UUID.randomUUID(),
                        normalizedEmail,
                        passwordEncoder.encode(request.password()),
                        now,
                        now,
                        UserStatus.ACTIVE,
                        UserRole.USER);

        User saved = userRepository.save(user);
        log.info("User registered successfully with id: {}", saved.getId());
        return UserResponse.from(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken =
                jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = generateRawRefreshToken();
        String tokenHash = GuestCookieUtil.sha256Hex(rawRefreshToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(refreshTokenExpirationSeconds));
        UUID familyId = UUID.randomUUID();

        RefreshToken token =
                new RefreshToken(
                        UUID.randomUUID(), user.getId(), tokenHash, expiresAt, null, now, familyId);
        refreshTokenRepository.save(token);

        log.info("User logged in successfully with id: {}", user.getId());
        return AuthResponse.bearer(
                accessToken, rawRefreshToken, jwtTokenProvider.getExpirationSeconds());
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = GuestCookieUtil.sha256Hex(request.refreshToken().strip());
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        Instant now = Instant.now();

        // Replay detection: If token was already revoked, revoke the entire family!
        if (token.isRevoked()) {
            log.warn(
                    "Replay attack detected for familyId: {}! Revoking all sessions in family.",
                    token.getFamilyId());
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            throw new UnauthorizedException("Refresh token reuse detected; all sessions revoked");
        }

        if (token.isExpired(now)) {
            throw new UnauthorizedException("Refresh token expired; please log in again");
        }

        // Revoke the current token upon use (rotation)
        token.setRevokedAt(now);
        refreshTokenRepository.save(token);

        User user =
                userRepository
                        .findById(token.getUserId())
                        .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Issue new access token and new rotated refresh token in the same family
        String newAccessToken =
                jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRawRefreshToken = generateRawRefreshToken();
        String newTokenHash = GuestCookieUtil.sha256Hex(newRawRefreshToken);
        Instant newExpiresAt = now.plus(Duration.ofSeconds(refreshTokenExpirationSeconds));

        RefreshToken newToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        user.getId(),
                        newTokenHash,
                        newExpiresAt,
                        null,
                        now,
                        token.getFamilyId());
        refreshTokenRepository.save(newToken);

        log.info("Token rotated successfully for user id: {}", user.getId());
        return AuthResponse.bearer(
                newAccessToken, newRawRefreshToken, jwtTokenProvider.getExpirationSeconds());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null
                && request.refreshToken() != null
                && !request.refreshToken().isBlank()) {
            String tokenHash = GuestCookieUtil.sha256Hex(request.refreshToken().strip());
            refreshTokenRepository
                    .findByTokenHash(tokenHash)
                    .ifPresent(
                            token -> {
                                token.setRevokedAt(Instant.now());
                                refreshTokenRepository.save(token);
                                log.info("Session revoked for user id: {}", token.getUserId());
                            });
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

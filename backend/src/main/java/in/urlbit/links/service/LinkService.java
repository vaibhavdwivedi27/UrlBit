package in.urlbit.links.service;

import in.urlbit.common.error.AliasTakenException;
import in.urlbit.common.error.GuestQuotaExceededException;
import in.urlbit.common.error.ResourceNotFoundException;
import in.urlbit.common.error.TakedownActiveException;
import in.urlbit.common.error.UnsafeDestinationException;
import in.urlbit.config.RateLimitProperties;
import in.urlbit.links.cache.CachedLink;
import in.urlbit.links.cache.LinkCacheService;
import in.urlbit.links.domain.DisabledReason;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.RedirectDestination;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import in.urlbit.links.event.LinkInvalidatedEvent;
import in.urlbit.links.repository.GuestQuotaRepository;
import in.urlbit.links.repository.ShortUrlRepository;
import in.urlbit.ratelimit.service.DistributedRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private static final Logger log = LoggerFactory.getLogger(LinkService.class);
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{4,32}$");
    private static final int MAX_GENERATION_ATTEMPTS = 3;
    private static final Duration GUEST_LINK_TTL = Duration.ofDays(7);

    private final ShortUrlRepository shortUrlRepository;
    private final GuestQuotaRepository guestQuotaRepository;
    private final Base62CodeGenerator codeGenerator;
    private final ReservedPathValidator reservedPathValidator;
    private final DestinationSafetyValidator safetyValidator;
    private final SafeBrowsingClient safeBrowsingClient;
    private final DistributedRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final LinkCacheService linkCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final String baseUrl;

    public LinkService(
            ShortUrlRepository shortUrlRepository,
            GuestQuotaRepository guestQuotaRepository,
            Base62CodeGenerator codeGenerator,
            ReservedPathValidator reservedPathValidator,
            DestinationSafetyValidator safetyValidator,
            SafeBrowsingClient safeBrowsingClient,
            DistributedRateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties,
            LinkCacheService linkCacheService,
            ApplicationEventPublisher eventPublisher,
            @Value("${urlbit.base-url:https://urlbit.in}") String baseUrl) {
        this.shortUrlRepository = shortUrlRepository;
        this.guestQuotaRepository = guestQuotaRepository;
        this.codeGenerator = codeGenerator;
        this.reservedPathValidator = reservedPathValidator;
        this.safetyValidator = safetyValidator;
        this.safeBrowsingClient = safeBrowsingClient;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.linkCacheService = linkCacheService;
        this.eventPublisher = eventPublisher;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public ShortUrlResponse create(
            CreateShortUrlRequest request, UUID ownerId, String guestIdHash, String remoteIp) {

        // Step 1: Destination safety validation outside the DB transaction
        safetyValidator.validate(request.longUrl());
        boolean isSafe = safeBrowsingClient.isSafe(request.longUrl(), ownerId != null);
        if (!isSafe) {
            throw new UnsafeDestinationException("Destination flagged as unsafe by Safe Browsing");
        }
        Instant now = Instant.now();
        Instant safetyCheckAt = isSafe ? now : null;

        // Step 2: Guest-specific rules and atomic quota increment
        if (ownerId == null) {
            String ipKey = rateLimiter.deriveIpKey(remoteIp);
            rateLimiter.checkLimit(
                    "rl:create:ip:" + ipKey,
                    rateLimitProperties.getCreate().getGuestLimit(),
                    rateLimitProperties.getCreate().getGuestWindow(),
                    true);

            if (request.customAlias() != null && !request.customAlias().isBlank()) {
                throw new UnsafeDestinationException(
                        "Custom aliases require an account. Please sign up to create custom links.");
            }
            if (request.expiresAt() != null) {
                throw new UnsafeDestinationException(
                        "Custom expiration requires an account. Guest links expire after 7 days.");
            }

            // Atomically increment the guest quota counter in PostgreSQL
            Short creations = guestQuotaRepository.incrementIfUnderQuota(guestIdHash);
            if (creations == null) {
                throw new GuestQuotaExceededException();
            }

            Instant guestExpiry = now.plus(GUEST_LINK_TTL);
            return createWithGeneratedCode(
                    null, request.longUrl(), guestExpiry, safetyCheckAt, now);
        }

        // Step 3: Authenticated creation
        rateLimiter.checkLimit(
                "rl:create:user:" + ownerId,
                rateLimitProperties.getCreate().getAuthenticatedLimit(),
                rateLimitProperties.getCreate().getAuthenticatedWindow(),
                true);

        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new UnsafeDestinationException("Expiration timestamp must be in the future");
        }

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            return createWithCustomAlias(
                    ownerId,
                    request.longUrl(),
                    request.customAlias().strip(),
                    request.expiresAt(),
                    safetyCheckAt,
                    now);
        }

        return createWithGeneratedCode(
                ownerId, request.longUrl(), request.expiresAt(), safetyCheckAt, now);
    }

    private ShortUrlResponse createWithCustomAlias(
            UUID ownerId,
            String longUrl,
            String alias,
            Instant expiresAt,
            Instant safetyCheckAt,
            Instant now) {

        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new UnsafeDestinationException(
                    "Custom alias must be 4-32 characters and contain only letters, numbers, hyphens, and underscores");
        }

        if (reservedPathValidator.isReserved(alias)) {
            throw new AliasTakenException(alias);
        }

        Long id =
                shortUrlRepository.insertOnConflictDoNothing(
                        ownerId,
                        alias,
                        longUrl,
                        now,
                        now,
                        expiresAt,
                        ShortUrlStatus.ACTIVE.name(),
                        null,
                        safetyCheckAt);

        if (id == null) {
            throw new AliasTakenException(alias);
        }

        ShortUrl entity = shortUrlRepository.findById(id).orElseThrow();
        eventPublisher.publishEvent(new LinkInvalidatedEvent(alias));
        return ShortUrlResponse.from(entity, baseUrl);
    }

    private ShortUrlResponse createWithGeneratedCode(
            UUID ownerId, String longUrl, Instant expiresAt, Instant safetyCheckAt, Instant now) {

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidateCode = codeGenerator.generate();
            if (reservedPathValidator.isReserved(candidateCode)) {
                continue;
            }

            Long id =
                    shortUrlRepository.insertOnConflictDoNothing(
                            ownerId,
                            candidateCode,
                            longUrl,
                            now,
                            now,
                            expiresAt,
                            ShortUrlStatus.ACTIVE.name(),
                            null,
                            safetyCheckAt);

            if (id != null) {
                ShortUrl entity = shortUrlRepository.findById(id).orElseThrow();
                return ShortUrlResponse.from(entity, baseUrl);
            }

            log.warn(
                    "Generated code collision on '{}' (attempt {}/{})",
                    candidateCode,
                    attempt,
                    MAX_GENERATION_ATTEMPTS);
        }

        throw new IllegalStateException(
                "Failed to generate a unique short code after "
                        + MAX_GENERATION_ATTEMPTS
                        + " attempts");
    }

    @Transactional(readOnly = true)
    public Optional<RedirectDestination> resolveRedirect(String code) {
        if (code == null || reservedPathValidator.isReserved(code)) {
            return Optional.empty();
        }

        Instant now = Instant.now();

        // Step 1: Cache-aside lookup in Redis
        Optional<CachedLink> cachedOpt = linkCacheService.get(code);
        if (cachedOpt.isPresent()) {
            CachedLink cached = cachedOpt.get();
            if (!cached.isRedirectable(now)) {
                return Optional.empty();
            }
            return Optional.of(new RedirectDestination(cached.id(), cached.originalUrl()));
        }

        // Step 2: Cache miss -> Read from PostgreSQL
        Optional<ShortUrl> linkOpt = shortUrlRepository.findByCode(code);
        if (linkOpt.isEmpty()) {
            // Negative cache entry to mitigate brute-force enumeration attacks
            linkCacheService.put(code, CachedLink.negative());
            return Optional.empty();
        }

        ShortUrl link = linkOpt.get();
        linkCacheService.put(code, CachedLink.of(link));

        if (!link.isRedirectable(now)) {
            return Optional.empty();
        }

        return Optional.of(new RedirectDestination(link.getId(), link.getOriginalUrl()));
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getById(Long id, UUID ownerId) {
        ShortUrl link =
                shortUrlRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Link not found with id: " + id));

        if (link.getStatus() == ShortUrlStatus.DELETED) {
            throw new ResourceNotFoundException("Link not found with id: " + id);
        }

        if (ownerId != null && !ownerId.equals(link.getOwnerId())) {
            throw new ResourceNotFoundException("Link not found with id: " + id);
        }

        return ShortUrlResponse.from(link, baseUrl);
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> listByOwner(UUID ownerId, Pageable pageable) {
        return shortUrlRepository
                .findByOwnerId(ownerId, pageable)
                .map(link -> ShortUrlResponse.from(link, baseUrl));
    }

    @Transactional
    public ShortUrlResponse update(Long id, UUID ownerId, UpdateShortUrlRequest request) {
        ShortUrl link =
                shortUrlRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Link not found with id: " + id));

        if (ownerId != null && !ownerId.equals(link.getOwnerId())) {
            throw new ResourceNotFoundException("Link not found with id: " + id);
        }

        if (link.getDisabledReason() == DisabledReason.ABUSE_TAKEDOWN) {
            throw new TakedownActiveException();
        }

        Instant now = Instant.now();

        if (request.status() != null) {
            link.setStatus(request.status());
            if (request.status() == ShortUrlStatus.DISABLED) {
                link.setDisabledReason(DisabledReason.OWNER);
            } else if (request.status() == ShortUrlStatus.ACTIVE) {
                link.setDisabledReason(null);
            }
        }

        if (request.expiresAt() != null) {
            if (!request.expiresAt().isAfter(now)) {
                throw new UnsafeDestinationException("Expiration timestamp must be in the future");
            }
            link.setExpiresAt(request.expiresAt());
        }

        link.setUpdatedAt(now);
        ShortUrl saved = shortUrlRepository.save(link);
        eventPublisher.publishEvent(new LinkInvalidatedEvent(link.getCode()));
        return ShortUrlResponse.from(saved, baseUrl);
    }

    @Transactional
    public void delete(Long id, UUID ownerId) {
        ShortUrl link =
                shortUrlRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Link not found with id: " + id));

        if (ownerId != null && !ownerId.equals(link.getOwnerId())) {
            throw new ResourceNotFoundException("Link not found with id: " + id);
        }

        link.setStatus(ShortUrlStatus.DELETED);
        link.setUpdatedAt(Instant.now());
        shortUrlRepository.save(link);
        eventPublisher.publishEvent(new LinkInvalidatedEvent(link.getCode()));
    }

    @Transactional
    public void takedown(String code, String reason) {
        ShortUrl link =
                shortUrlRepository
                        .findByCode(code)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Link not found with code: " + code));

        link.setStatus(ShortUrlStatus.DISABLED);
        link.setDisabledReason(DisabledReason.ABUSE_TAKEDOWN);
        link.setUpdatedAt(Instant.now());
        shortUrlRepository.save(link);
        eventPublisher.publishEvent(new LinkInvalidatedEvent(code));
        log.info("Abuse takedown executed on code '{}' with reason: {}", code, reason);
    }

    @Transactional
    public int rescanBatch(int limit) {
        java.util.List<ShortUrl> candidates = shortUrlRepository.findBatchForSafetyRescan(limit);
        if (candidates.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        int disabledCount = 0;

        for (ShortUrl link : candidates) {
            boolean isSafe =
                    safeBrowsingClient.isSafe(link.getOriginalUrl(), link.getOwnerId() != null);
            if (!isSafe) {
                link.setStatus(ShortUrlStatus.DISABLED);
                link.setDisabledReason(DisabledReason.ABUSE_TAKEDOWN);
                eventPublisher.publishEvent(new LinkInvalidatedEvent(link.getCode()));
                disabledCount++;
                log.warn(
                        "Safety re-scan flagged link '{}' as unsafe! Disabled for abuse.",
                        link.getCode());
            }
            link.setLastSafetyCheckAt(now);
            link.setUpdatedAt(now);
            shortUrlRepository.save(link);
        }

        return disabledCount;
    }

    @Transactional
    public int purgeExpiredGuestQuotas() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        int purged = guestQuotaRepository.deleteOlderThan(cutoff);
        log.info("Purged {} expired guest quota records older than 30 days", purged);
        return purged;
    }
}

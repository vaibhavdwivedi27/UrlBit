package in.urlbit.links.dto;

import in.urlbit.links.domain.ShortUrlStatus;
import java.time.Instant;

public record UpdateShortUrlRequest(ShortUrlStatus status, Instant expiresAt) {}

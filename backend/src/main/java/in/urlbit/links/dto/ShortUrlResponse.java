package in.urlbit.links.dto;

import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import java.time.Instant;

public record ShortUrlResponse(
        Long id,
        String code,
        String shortUrl,
        String longUrl,
        ShortUrlStatus status,
        Instant expiresAt,
        Instant createdAt) {
    public static ShortUrlResponse from(ShortUrl entity, String baseUrl) {
        String canonicalShortUrl = baseUrl + "/" + entity.getCode();
        return new ShortUrlResponse(
                entity.getId(),
                entity.getCode(),
                canonicalShortUrl,
                entity.getOriginalUrl(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt());
    }
}

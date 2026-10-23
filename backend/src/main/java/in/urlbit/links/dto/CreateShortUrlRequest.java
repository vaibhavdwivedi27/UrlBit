package in.urlbit.links.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateShortUrlRequest(
        @NotBlank(message = "Destination URL is required")
                @Size(max = 2048, message = "URL length exceeds limit of 2048 characters")
                String longUrl,
        @Size(min = 4, max = 32, message = "Custom alias must be between 4 and 32 characters")
                String customAlias,
        Instant expiresAt) {}

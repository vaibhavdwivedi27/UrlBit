package in.urlbit.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.common.error.TakedownActiveException;
import in.urlbit.links.domain.DisabledReason;
import in.urlbit.links.domain.ShortUrl;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import in.urlbit.links.repository.ShortUrlRepository;
import in.urlbit.links.service.LinkService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TakedownIT extends AbstractIntegrationTest {

    @Autowired private LinkService linkService;

    @Autowired private ShortUrlRepository shortUrlRepository;

    @Autowired private in.urlbit.auth.repository.UserRepository userRepository;

    @Test
    void takedownDisablesLinkAndOwnerCannotReEnable() {
        String code = "take123";
        var user = createTestUser(userRepository, "takedown-" + UUID.randomUUID() + "@example.com");
        UUID ownerId = user.getId();

        ShortUrl link =
                new ShortUrl(
                        ownerId,
                        code,
                        "https://example.com/malicious",
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.ACTIVE,
                        null,
                        Instant.now());
        ShortUrl saved = shortUrlRepository.save(link);

        // Operator takedown
        linkService.takedown(code, "Confirmed phishing campaign");

        ShortUrl afterTakedown = shortUrlRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterTakedown.getStatus()).isEqualTo(ShortUrlStatus.DISABLED);
        assertThat(afterTakedown.getDisabledReason()).isEqualTo(DisabledReason.ABUSE_TAKEDOWN);

        // Owner attempts to re-enable link via update
        var updateRequest = new UpdateShortUrlRequest(ShortUrlStatus.ACTIVE, null);
        assertThatThrownBy(() -> linkService.update(saved.getId(), ownerId, updateRequest))
                .isInstanceOf(TakedownActiveException.class)
                .hasMessageContaining("taken down for abuse");
    }
}

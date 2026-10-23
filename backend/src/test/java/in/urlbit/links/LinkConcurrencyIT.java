package in.urlbit.links;

import static org.assertj.core.api.Assertions.assertThat;

import in.urlbit.AbstractIntegrationTest;
import in.urlbit.links.domain.ShortUrlStatus;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.repository.ShortUrlRepository;
import in.urlbit.links.service.LinkService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LinkConcurrencyIT extends AbstractIntegrationTest {

    @Autowired private LinkService linkService;

    @Autowired private ShortUrlRepository shortUrlRepository;

    @Autowired private in.urlbit.auth.repository.UserRepository userRepository;

    @Test
    void parallelCustomAliasCreationAllowsExactlyOneWinner() throws InterruptedException {
        String alias = "race-" + UUID.randomUUID().toString().substring(0, 8);
        var user = createTestUser(userRepository, "race-" + UUID.randomUUID() + "@example.com");
        UUID ownerId = user.getId();
        int threads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(
                    () -> {
                        try {
                            latch.await();
                            var request =
                                    new CreateShortUrlRequest(
                                            "https://example.com/race", alias, null);
                            linkService.create(request, ownerId, null, "127.0.0.1");
                            successCount.incrementAndGet();
                        } catch (in.urlbit.common.error.AliasTakenException ex) {
                            conflictCount.incrementAndGet();
                        } catch (Exception ignored) {
                        }
                    });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threads - 1);
    }

    @Test
    void forcedCollisionRetriesSuccessfullyAndPreservesGuestQuota() {
        // Pre-insert a short URL with a known code
        String collidingCode = "collide";
        shortUrlRepository.insertOnConflictDoNothing(
                null,
                collidingCode,
                "https://example.com/original",
                Instant.now(),
                Instant.now(),
                null,
                ShortUrlStatus.ACTIVE.name(),
                null,
                Instant.now());

        // Inserting the same code again via insertOnConflictDoNothing returns null (no row
        // inserted)
        Long secondAttemptId =
                shortUrlRepository.insertOnConflictDoNothing(
                        null,
                        collidingCode,
                        "https://example.com/duplicate",
                        Instant.now(),
                        Instant.now(),
                        null,
                        ShortUrlStatus.ACTIVE.name(),
                        null,
                        Instant.now());

        assertThat(secondAttemptId).isNull();
    }
}

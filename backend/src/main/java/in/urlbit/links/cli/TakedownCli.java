package in.urlbit.links.cli;

import in.urlbit.links.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Operator CLI runner for abuse takedowns without requiring administrative HTTP endpoints.
 *
 * <p>Usage: java -jar urlbit.jar --takedown.code=abc123 --takedown.reason="Phishing report"
 */
@Component
public class TakedownCli implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TakedownCli.class);

    private final LinkService linkService;

    public TakedownCli(LinkService linkService) {
        this.linkService = linkService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("takedown.code")) {
            String code = args.getOptionValues("takedown.code").getFirst();
            String reason =
                    args.containsOption("takedown.reason")
                            ? args.getOptionValues("takedown.reason").getFirst()
                            : "Operator abuse takedown";

            try {
                linkService.takedown(code, reason);
                log.info("SUCCESS: Short URL with code '{}' has been disabled for abuse", code);
            } catch (Exception ex) {
                log.error(
                        "ERROR: Failed to perform takedown on code '{}': {}",
                        code,
                        ex.getMessage());
            }
        }
    }
}

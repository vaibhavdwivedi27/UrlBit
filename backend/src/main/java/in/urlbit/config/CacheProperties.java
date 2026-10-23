package in.urlbit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "urlbit.cache")
public class CacheProperties {

    private Duration linkTtl = Duration.ofHours(1);
    private Duration negativeTtl = Duration.ofSeconds(30);

    public Duration getLinkTtl() {
        return linkTtl;
    }

    public void setLinkTtl(Duration linkTtl) {
        this.linkTtl = linkTtl;
    }

    public Duration getNegativeTtl() {
        return negativeTtl;
    }

    public void setNegativeTtl(Duration negativeTtl) {
        this.negativeTtl = negativeTtl;
    }
}

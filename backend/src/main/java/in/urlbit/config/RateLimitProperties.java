package in.urlbit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "urlbit.rate-limit")
public class RateLimitProperties {

    private Create create = new Create();
    private Login login = new Login();

    public Create getCreate() {
        return create;
    }

    public void setCreate(Create create) {
        this.create = create;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public static class Create {
        private int authenticatedLimit = 30;
        private Duration authenticatedWindow = Duration.ofSeconds(60);
        private int guestLimit = 30;
        private Duration guestWindow = Duration.ofSeconds(60);

        public int getAuthenticatedLimit() {
            return authenticatedLimit;
        }

        public void setAuthenticatedLimit(int authenticatedLimit) {
            this.authenticatedLimit = authenticatedLimit;
        }

        public Duration getAuthenticatedWindow() {
            return authenticatedWindow;
        }

        public void setAuthenticatedWindow(Duration authenticatedWindow) {
            this.authenticatedWindow = authenticatedWindow;
        }

        public int getGuestLimit() {
            return guestLimit;
        }

        public void setGuestLimit(int guestLimit) {
            this.guestLimit = guestLimit;
        }

        public Duration getGuestWindow() {
            return guestWindow;
        }

        public void setGuestWindow(Duration guestWindow) {
            this.guestWindow = guestWindow;
        }
    }

    public static class Login {
        private int limit = 10;
        private Duration window = Duration.ofSeconds(300);

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}

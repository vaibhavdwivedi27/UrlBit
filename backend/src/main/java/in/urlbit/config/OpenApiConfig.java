package in.urlbit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlbitOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Urlbit API")
                                .version("v1")
                                .description(
                                        "Urlbit \u2014 URL shortening and link analytics API"));
    }
}

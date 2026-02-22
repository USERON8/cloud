package com.cloud.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(OpenAPI.class)
public abstract class BaseKnife4jConfig {

    protected OpenAPI createOpenAPI(String title, String description, String version) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact()
                                .name("Cloud Team")
                                .email("cloud@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost"),
                        new Server().url("https://localhost")
                ));
    }

    protected abstract String getServiceTitle();

    protected abstract String getServiceDescription();

    protected String getServiceVersion() {
        return "1.0.0";
    }

    @Bean
    public OpenAPI defaultOpenAPI() {
        return createOpenAPI(getServiceTitle(), getServiceDescription(), getServiceVersion());
    }
}

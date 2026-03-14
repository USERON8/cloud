package com.cloud.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(OpenAPI.class)
public abstract class BaseKnife4jConfig {

    protected OpenAPI createOpenAPI(String title, String description, String version) {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Bearer Token");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("Authorization", jwtScheme))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
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
                        new Server().url("http://127.0.0.1"),
                        new Server().url("https://127.0.0.1")
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

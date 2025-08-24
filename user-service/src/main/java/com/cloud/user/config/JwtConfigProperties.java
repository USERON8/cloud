package com.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
public class JwtConfigProperties {
    /**
     * JWK端点URI
     */
    private String jwkSetUri;
}
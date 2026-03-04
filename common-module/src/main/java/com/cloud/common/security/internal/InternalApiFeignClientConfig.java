package com.cloud.common.security.internal;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalApiFeignClientConfig {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Value("${cloud.security.internal-api.header-name:" + InternalApiHeaders.API_KEY_HEADER + "}")
    private String headerName;

    @Value("${cloud.security.internal-api.caller-header-name:" + InternalApiHeaders.CALLER_HEADER + "}")
    private String callerHeaderName;

    @Value("${cloud.security.internal-api.key:${INTERNAL_API_KEY:change-me-internal-api-key}}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyRequestInterceptor() {
        return template -> {
            String path = template.path();
            if (path != null && path.startsWith("/internal/")) {
                template.header(headerName, internalApiKey);
                template.header(callerHeaderName, applicationName);
            }
        };
    }
}

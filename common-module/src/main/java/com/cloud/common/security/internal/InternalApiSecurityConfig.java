package com.cloud.common.security.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Slf4j
@Configuration
@EnableConfigurationProperties(InternalApiSecurityProperties.class)
@ConditionalOnProperty(prefix = "cloud.security.internal-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InternalApiSecurityConfig {

    @Bean
    public FilterRegistrationBean<InternalApiKeyAuthFilter> internalApiKeyAuthFilter(InternalApiSecurityProperties properties) {
        if ("change-me-internal-api-key".equals(properties.getKey())) {
            log.warn("Using default INTERNAL_API_KEY. Set env INTERNAL_API_KEY in all services.");
        }
        FilterRegistrationBean<InternalApiKeyAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new InternalApiKeyAuthFilter(properties));
        bean.addUrlPatterns("/internal/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}

package com.cloud.auth.config;

import com.cloud.auth.service.OAuth2ClientCredentialsService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    @Lazy
    private final OAuth2ClientCredentialsService oauth2ClientCredentialsService;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            if (isInternalUserServiceRequest(template)) {
                template.header("Authorization");
                injectInternalApiToken(template);
                return;
            }

            if (template.headers().containsKey("Authorization")) {
                log.debug("Skip Authorization injection because request already has Authorization header");
                return;
            }

            if (isAuthenticating()) {
                log.debug("Authentication flow in progress, skip Authorization header injection");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                template.header("Authorization", "Bearer " + token);
                log.debug("Injected user JWT token into Feign request");
                return;
            }

            log.debug("No authentication context found, skip Authorization header injection");
        };
    }

    private boolean isInternalUserServiceRequest(RequestTemplate template) {
        return true;
    }

    private void injectInternalApiToken(RequestTemplate template) {
        String internalToken = oauth2ClientCredentialsService.getInternalApiToken();
        if (internalToken == null || internalToken.isBlank()) {
            log.warn("Failed to inject internal API token for user-service internal request");
            return;
        }
        template.header("Authorization", "Bearer " + internalToken);
        log.debug("Injected internal API token into user-service internal request");
    }

    private boolean isAuthenticating() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("OAuth2") && className.contains("authenticate")) {
                return true;
            }
        }
        return false;
    }
}

package com.cloud.auth.config;

import com.cloud.auth.service.OAuth2ClientCredentialsService;
import feign.RequestInterceptor;
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
            
            if (template.headers().containsKey("Authorization")) {
                log.debug("璇锋眰宸插寘鍚獳uthorization澶达紝璺宠繃娣诲姞");
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
                log.debug("Feign璇锋眰娣诲姞JWT浠ょ墝鍒拌姹傚ご: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            } else {
                
                
                if (template.feignTarget() != null && template.feignTarget().url().contains("user-service") &&
                        template.url().contains("/internal/")) {
                    log.debug("妫€娴嬪埌鍐呴儴API璋冪敤锛屽皾璇曚娇鐢ㄥ鎴风鍑瘉妯″紡鑾峰彇浠ょ墝");
                    String internalToken = oauth2ClientCredentialsService.getInternalApiToken();
                    if (internalToken != null) {
                        template.header("Authorization", "Bearer " + internalToken);
                        log.debug("鎴愬姛娣诲姞鍐呴儴API浠ょ墝鍒拌姹傚ご");
                    } else {
                        log.warn("鏃犳硶鑾峰彇鍐呴儴API浠ょ墝锛屽皢浠ユ棤璁よ瘉鏂瑰紡璋冪敤");
                    }
                } else {
                    log.debug("No authentication context found, skip Authorization header injection");
                }
            }
        };
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

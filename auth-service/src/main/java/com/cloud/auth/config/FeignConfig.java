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

    /**
     * Feign请求拦截器，用于在Feign调用时添加认证头
     * 暂时简化，直接传递JWT token，避免循环依赖
     *
     * @return RequestInterceptor 请求拦截器
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 检查是否已经有Authorization头
            if (template.headers().containsKey("Authorization")) {
                log.debug("请求已包含Authorization头，跳过添加");
                return;
            }

            // 检查当前线程是否正在进行认证流程，避免循环调用
            if (isAuthenticating()) {
                log.debug("当前正在认证流程中，跳过添加Authorization头");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 如果当前上下文中存在JWT认证信息，则添加到请求头中
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                template.header("Authorization", "Bearer " + token);
                log.debug("Feign请求添加JWT令牌到请求头: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            } else {
                // 当前无认证上下文时，尝试使用客户端凭证模式获取内部API令牌
                // 主要用于auth-service调用user-service的场景
                if (template.feignTarget() != null && template.feignTarget().url().contains("user-service") &&
                        template.url().contains("/internal/")) {
                    log.debug("检测到内部API调用，尝试使用客户端凭证模式获取令牌");
                    String internalToken = oauth2ClientCredentialsService.getInternalApiToken();
                    if (internalToken != null) {
                        template.header("Authorization", "Bearer " + internalToken);
                        log.debug("成功添加内部API令牌到请求头");
                    } else {
                        log.warn("无法获取内部API令牌，将以无认证方式调用");
                    }
                } else {
                    log.debug("当前无认证上下文，跳过添加Authorization头");
                }
            }
        };
    }

    /**
     * 检查当前线程是否正在进行认证流程，避免循环调用
     *
     * @return boolean 是否正在进行认证
     */
    private boolean isAuthenticating() {
        // 检查调用栈中是否包含认证相关类，避免循环调用
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 如果调用栈中包含认证相关的类，则认为正在认证流程中
            if (className.contains("OAuth2") && className.contains("authenticate")) {
                return true;
            }
        }
        return false;
    }
}
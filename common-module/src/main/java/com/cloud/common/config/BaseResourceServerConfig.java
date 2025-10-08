package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2资源服务器基础配置
 * 提供通用的JWT验证和权限配置，减少各服务的重复代码
 * 
 * @author what's up
 * @since 2025-10-05
 */
@Slf4j
public abstract class BaseResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://127.0.0.1:80/.well-known/jwks.json}")
    private String jwkSetUri;

    /**
     * 配置安全过滤器链
     * 子类可以覆盖此方法来定制自己的安全规则
     */
    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String serviceName = getServiceName();
        log.info("🔧 配置{}的OAuth2.1资源服务器安全过滤器链", serviceName);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowCredentials(true);
                config.addAllowedOriginPattern("*");
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                return config;
            }))
            .authorizeHttpRequests(authz -> {
                // 配置公共端点
                configurePublicEndpoints(authz);
                // 配置服务特定的端点
                configureServiceEndpoints(authz);
                // 其他请求需要认证
                authz.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("🔒 JWT认证失败: {}", authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"unauthorized\",\"message\":\"JWT令牌无效或已过期\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("🚫 JWT授权失败: {}", accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"access_denied\",\"message\":\"权限不足\"}"
                    );
                })
            );

        log.info("✅ {}OAuth2.1资源服务器安全过滤器链配置完成", serviceName);
        return http.build();
    }

    /**
     * 配置公共端点（所有服务通用）
     */
    protected void configurePublicEndpoints(
        org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz
            // 健康检查和监控
            .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
            // API文档
            .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll();
    }

    /**
     * 配置服务特定的端点权限
     * 子类必须实现此方法来定义自己的端点权限
     */
    protected abstract void configureServiceEndpoints(
        org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz);

    /**
     * 获取服务名称
     * 用于日志记录
     */
    protected abstract String getServiceName();

    /**
     * JWT解码器配置
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("🔧 配置{}JWT解码器，JWK端点: {}", getServiceName(), jwkSetUri);
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * JWT认证转换器配置
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // OAuth2.1标准：从scope字段中提取权限，使用SCOPE_前缀
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");
        
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return converter;
    }
}

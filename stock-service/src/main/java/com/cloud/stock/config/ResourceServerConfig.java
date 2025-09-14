package com.cloud.stock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock Service OAuth2资源服务器配置
 * 与OAuth2.1框架集成，支持JWT Token验证和权限控制
 * 
 * @author what's up
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.cache-duration:PT30M}")
    private String jwtCacheDuration;

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 公开访问的端点
                        .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        // Swagger和API文档端点
                        .requestMatchers("/doc.html/**", "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                        
                        // 内部API需要internal_api scope
                        .requestMatchers("/api/stock/internal/**").hasAuthority("SCOPE_internal_api")
                        
                        // 库存查询接口需要read scope
                        .requestMatchers("/api/stock/query/**").hasAnyAuthority("SCOPE_read", "SCOPE_user.read")
                        
                        // 库存管理接口需要write scope
                        .requestMatchers("/api/stock/manage/**").hasAnyAuthority("SCOPE_write", "SCOPE_user.write")
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));

        return http.build();
    }

    /**
     * JWT解码器
     * 使用OAuth2提供的JWK端点进行JWT验证
     *
     * @return NimbusJwtDecoder JWT解码器实例
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();

        // JWT验证器 - 使用OAuth2.1标准配置
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator("http://127.0.0.1:80")); // 使用网关的issuer

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(validators);
        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}
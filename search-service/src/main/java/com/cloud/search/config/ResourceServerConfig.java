package com.cloud.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索服务OAuth2资源服务器配置
 * <p>
 * 主要功能：
 * - JWT验证和解码
 * - 权限提取和转换
 * - 搜索服务特定的安全规则
 * - 公开路径和受保护路径配置
 *
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-10-03
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.cache-duration:PT30M}")
    private String jwtCacheDuration;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String jwtIssuer;

    /**
     * 配置搜索服务的安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔍 配置搜索服务安全过滤器链");

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 公开路径 - 不需要认证
                    auth.requestMatchers(
                            "/actuator/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/doc.html",
                            "/webjars/**",
                            "/favicon.ico"
                    ).permitAll();

                    // 内部API - 需要内部权限
                    auth.requestMatchers("/search/internal/**")
                            .hasAuthority("SCOPE_internal_api");

                    // 管理API - 需要管理员权限
                    auth.requestMatchers("/search/manage/**")
                            .hasRole("ADMIN");

                    // 查询API - 需要认证但允许所有角色
                    auth.requestMatchers("/search/query/**", "/search/suggest/**", "/search/hot/**")
                            .authenticated();

                    // 其他所有请求都需要认证
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .build();
    }

    /**
     * 创建JWT解码器
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();

        // 创建验证器列表
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();

        // 添加默认验证器（包含发行者验证）
        validators.add(JwtValidators.createDefaultWithIssuer(jwtIssuer));

        // 设置组合验证器
        if (validators.size() > 1) {
            OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(validators);
            jwtDecoder.setJwtValidator(combinedValidator);
        } else if (!validators.isEmpty()) {
            jwtDecoder.setJwtValidator(validators.get(0));
        }

        log.info("✅ 搜索服务JWT解码器配置完成，验证器数量: {}", validators.size());
        return jwtDecoder;
    }

    /**
     * 创建JWT认证转换器
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // OAuth2.1标准：从 scope 声明中提取权限
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        log.info("✅ 搜索服务JWT认证转换器配置完成");
        return converter;
    }

    /**
     * 记录配置完成
     */
    @Bean
    public String logSearchSecurityConfiguration() {
        log.info("✅ 搜索服务 OAuth2资源服务器配置完成");
        log.info("   - JWT验证端点: {}", jwkSetUri);
        log.info("   - JWT缓存时间: {}", jwtCacheDuration);
        log.info("   - JWT发行者: {}", jwtIssuer);
        log.info("   - 权限前缀: SCOPE_");
        log.info("   - 权限声明: scope");
        log.info("   - 支持的API类型: 查询API、管理API、内部API");
        return "search-security-configured";
    }
}

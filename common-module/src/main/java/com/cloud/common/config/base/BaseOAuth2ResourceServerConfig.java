package com.cloud.common.config.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2资源服务器配置基类
 * 提供统一的JWT验证和权限控制配置，各微服务继承此类并根据需要自定义配置
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>统一的JWT解码器配置</li>
 *   <li>标准化的权限提取机制</li>
 *   <li>可扩展的自定义验证器支持</li>
 *   <li>统一的异常处理配置</li>
 * </ul>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public abstract class BaseOAuth2ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    protected String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.cache-duration:PT30M}")
    protected String jwtCacheDuration;

    @Value("${app.jwt.issuer:http://localhost:8080}")
    protected String jwtIssuer;

    /**
     * 配置安全过滤器链
     * 子类可以重写此方法来自定义安全配置
     */
    protected SecurityFilterChain createSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 配置公开路径
                    configurePublicPaths(auth);
                    // 配置受保护路径
                    configureProtectedPaths(auth);
                    // 其他所有请求都需要认证
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(createJwtDecoder())
                                .jwtAuthenticationConverter(createJwtAuthenticationConverter()))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .build();
    }

    /**
     * 配置公开路径（不需要认证）
     * 子类重写此方法来定义服务特定的公开路径
     */
    protected abstract void configurePublicPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth);

    /**
     * 配置受保护路径（需要特定权限）
     * 子类重写此方法来定义服务特定的权限要求
     */
    protected abstract void configureProtectedPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth);

    /**
     * 创建JWT解码器
     * 配置JWT验证规则和缓存策略，子类可以重写此方法来添加自定义验证器
     *
     * @return 配置好的JWT解码器
     * @author what's up
     * @date 2025-01-15
     */
    protected JwtDecoder createJwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();

        // 创建验证器列表
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        
        // 添加默认验证器
        validators.add(JwtValidators.createDefaultWithIssuer(jwtIssuer));
        
        // 添加自定义验证器
        addCustomValidators(validators);

        // 设置组合验证器
        if (validators.size() > 1) {
            OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(validators);
            jwtDecoder.setJwtValidator(combinedValidator);
        } else if (!validators.isEmpty()) {
            jwtDecoder.setJwtValidator(validators.get(0));
        }

        log.info("✅ JWT解码器配置完成，验证器数量: {}", validators.size());
        return jwtDecoder;
    }

    /**
     * 添加自定义JWT验证器
     * 子类重写此方法来添加特定的验证器（如黑名单检查）
     */
    protected void addCustomValidators(List<OAuth2TokenValidator<Jwt>> validators) {
        // 默认不添加自定义验证器，子类可以重写
    }

    /**
     * 创建JWT认证转换器
     * 配置如何从JWT中提取权限信息
     */
    protected JwtAuthenticationConverter createJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // OAuth2.1标准：从 scope 声明中提取权限
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return converter;
    }

    /**
     * 获取服务名称，用于日志记录
     */
    protected abstract String getServiceName();

    /**
     * 记录配置完成日志
     */
    protected void logConfigurationComplete() {
        log.info("✅ {} OAuth2资源服务器配置完成", getServiceName());
        log.info("   - JWT验证端点: {}", jwkSetUri);
        log.info("   - JWT缓存时间: {}", jwtCacheDuration);
        log.info("   - JWT发行者: {}", jwtIssuer);
    }
}

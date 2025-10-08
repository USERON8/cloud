package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 用户服务 OAuth2.1资源服务器配置
 * 独立的OAuth2资源服务器配置，不依赖common-module
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final TokenBlacklistChecker tokenBlacklistChecker;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://127.0.0.1:80/.well-known/jwks.json}")
    private String jwkSetUri;

    /**
     * 配置用户服务的安全过滤器链
     */
    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 配置用户服务OAuth2.1资源服务器安全过滤器链");

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
                .authorizeHttpRequests(authz -> authz
                        // 公共端点放行
                        .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()

                        // 内部API需要internal_api scope
                        .requestMatchers("/user/internal/**")
                        .hasAuthority("SCOPE_internal_api")

                        // 管理员管理接口 - 需要管理员权限
                        .requestMatchers("/admin/manage/**")
                        .hasAuthority("SCOPE_admin")

                        // 用户管理接口 - 需要管理权限
                        .requestMatchers("/user/manage/**")
                        .hasAuthority("SCOPE_admin")

                        // 用户资料接口 - 用户可以访问自己的或管理员可以访问任何用户的
                        .requestMatchers("/api/user/profile/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        // 用户地址接口 - 用户权限
                        .requestMatchers("/api/user/address/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        // 商户相关接口 - 商户权限或管理员权限
                        .requestMatchers("/api/user/merchant/**")
                        .hasAnyAuthority("SCOPE_merchant", "SCOPE_admin")

                        // 权限示例接口需要认证
                        .requestMatchers("/example/permissions/**").authenticated()

                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
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

        log.info("✅ 用户服务OAuth2.1资源服务器安全过滤器链配置完成");
        return http.build();
    }

    /**
     * JWT解码器配置
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("🔧 配置用户服务JWT解码器，JWK端点: {}", jwkSetUri);
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // 集成令牌黑名单验证器
        decoder.setJwtValidator(tokenBlacklistChecker);

        return decoder;
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

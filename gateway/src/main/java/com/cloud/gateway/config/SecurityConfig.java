package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 网关安全配置类
 * 配置Spring Cloud Gateway作为OAuth2.1资源服务器的统一鉴权
 * 
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * 配置安全过滤器链
     *
     * @param http ServerHttpSecurity对象
     * @return SecurityWebFilterChain 安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // OAuth2.1 授权服务器端点 - 放行
                        .pathMatchers("/oauth2/**", "/.well-known/**").permitAll()
                        // 认证相关端点 - 放行
                        .pathMatchers("/auth/login", "/auth/register", "/auth/captcha").permitAll()
                        // 管理端点 - 放行
                        .pathMatchers("/actuator/**").permitAll()
                        // API文档端点 - 放行
                        .pathMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        // 静态资源 - 放行
                        .pathMatchers("/favicon.ico", "/static/**").permitAll()
                        // 所有其他请求需要鉴权
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .headers(headers -> headers
                        .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((swe, ex) -> {
                            log.warn("网关鉴权失败: {}", ex.getMessage());
                            swe.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            swe.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");

                            String body = "{\"code\":401,\"error\":\"Unauthorized\",\"message\":\"认证失败，请提供有效的JWT Token\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            var buffer = swe.getResponse().bufferFactory().wrap(body.getBytes());
                            return swe.getResponse().writeWith(Mono.just(buffer));
                        })
                        .accessDeniedHandler((swe, denied) -> {
                            log.warn("网关权限不足: {}", denied.getMessage());
                            swe.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                            swe.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");

                            String body = "{\"code\":403,\"error\":\"Forbidden\",\"message\":\"权限不足，无法访问该资源\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            var buffer = swe.getResponse().bufferFactory().wrap(body.getBytes());
                            return swe.getResponse().writeWith(Mono.just(buffer));
                        })
                );

        return http.build();
    }

    /**
     * JWT解码器
     * 使用OAuth2提供的JWK端点进行JWT验证
     *
     * @return ReactiveJwtDecoder JWT解码器实例
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        log.info("配置JWT解码器，JWK端点: {}", jwkSetUri);
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * JWT认证转换器
     * 配置OAuth2.1标准的JWT权限提取和转换
     *
     * @return ReactiveJwtAuthenticationConverter JWT认证转换器
     */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // OAuth2.1标准：从 scope 声明中提取权限
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        
        // 设置权限转换器
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            log.debug("JWT Token 权限转换: subject={}, scope={}", 
                    jwt.getClaimAsString("sub"), jwt.getClaimAsString("scope"));
            return Flux.fromIterable(authoritiesConverter.convert(jwt));
        });

        return jwtConverter;
    }
}

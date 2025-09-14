package com.cloud.user.config;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;

/**
 * User Service OAuth2资源服务器配置
 * 与OAuth2.1框架集成，支持JWT Token验证和权限控制
 * 
 * @author what's up
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
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
                        // 公共端点放行
                        .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()

                        // 用户注册登录接口放行（这些应该通过auth-service处理）
                        .requestMatchers("/user/register", "/user/login").permitAll()

                        // 内部API需要internal_api scope或放行注册接口
                        .requestMatchers("/user/internal/register").permitAll()
                        // 放行findByUsername接口，供auth-service调用（避免循环依赖）
                        .requestMatchers("/user/internal/username/**").permitAll()
                        .requestMatchers("/user/internal/**").hasAuthority("SCOPE_internal_api")

                        // 用户管理接口 - 需要特定权限控制（已通过方法级权限控制）
                        .requestMatchers("/user/manage/**").authenticated()

                        // 管理员管理接口 - 需要管理员权限
                        .requestMatchers("/admin/manage/**").hasAuthority("ROLE_ADMIN")

                        // 用户相关接口需要对应的OAuth2.1 scope
                        .requestMatchers("/api/user/profile/**").hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write")
                        .requestMatchers("/api/user/address/**").hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write")

                        // 商户相关接口需要merchant scope
                        .requestMatchers("/api/user/merchant/**").hasAnyAuthority("SCOPE_read", "SCOPE_write")

                        // 权限示例接口需要认证
                        .requestMatchers("/example/permissions/**").authenticated()

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
     * 使用OAuth2提供的JWK端点进行JWT验证，配置缓存和验证器
     *
     * @return NimbusJwtDecoder JWT解码器实例
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // 使用配置文件中的JWK Set URI构建解码器
        Duration cacheDuration = Duration.parse(jwtCacheDuration);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();

        // 注意：这里先移除cache调用，因为可能的API变化问题
        // 如果需要，可以通过其他方式配置缓存

        // 配置JWT验证器
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator
                // 可以在这里添加更多的验证器，例如自定义的权限验证器
        );

        jwtDecoder.setJwtValidator(combinedValidator);

        return jwtDecoder;
    }
}
package com.cloud.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全过滤器链配置
 * 严格遵循OAuth2.1标准，分离授权服务器和资源服务器的安全配置
 * <p>
 * 配置优先级:
 * 1. OAuth2授权服务器过滤器链 (Order = 1)
 * 2. 资源服务器过滤器链 (Order = 2)
 * 3. 默认过滤器链 (Order = 3)
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class SecurityFilterChainConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityFilterChainConfig(JwtDecoder jwtDecoder,
                                     @Qualifier("enhancedJwtAuthenticationConverter") JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * OAuth2.1授权服务器安全过滤器链
     * 处理OAuth2授权服务器的所有端点
     * <p>
     * 处理的端点:
     * - /oauth2/authorize (授权端点)
     * - /oauth2/token (令牌端点)
     * - /oauth2/revoke (令牌撤销端点)
     * - /oauth2/introspect (令牌内省端点)
     * - /oauth2/jwks (JWK集合端点)
     * - /.well-known/oauth-authorization-server (发现端点)
     * - /connect/logout (OpenID Connect登出端点)
     * - /userinfo (用户信息端点)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 配置OAuth2.1授权服务器安全过滤器链");

        // 创建OAuth2授权服务器配置器
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        // 启用OpenID Connect支持
        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        http
                // 匹配OAuth2和OpenID Connect相关端点
                .securityMatcher(
                        "/oauth2/**",
                        "/.well-known/**",
                        "/connect/**",
                        "/userinfo"
                )

                // 应用OAuth2授权服务器配置
                .with(authorizationServerConfigurer, Customizer.withDefaults())

                // OAuth2.1安全配置
                .csrf(AbstractHttpConfigurer::disable)  // OAuth2不需要CSRF保护
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*");
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))

                // 授权配置
                .authorizeHttpRequests(authorize -> authorize
                        // 公开端点（OAuth2.1标准要求）
                        .requestMatchers(
                                "/.well-known/**",           // 发现端点
                                "/oauth2/token",             // 令牌端点
                                "/oauth2/jwks",              // JWK集合端点
                                "/oauth2/revoke",            // 令牌撤销端点
                                "/oauth2/introspect"         // 令牌内省端点
                        ).permitAll()

                        // 需要认证的端点
                        .requestMatchers(
                                "/oauth2/authorize",         // 授权端点
                                "/connect/**",               // OpenID Connect端点
                                "/userinfo"                  // 用户信息端点
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                // OAuth2.1认证方式
                .httpBasic(Customizer.withDefaults())  // 支持HTTP Basic认证
                .formLogin(Customizer.withDefaults())  // 支持表单登录（用于授权页面）

                // OAuth2.1会话管理
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // 授权服务器需要会话支持
                        .maximumSessions(1)  // 限制并发会话
                        .maxSessionsPreventsLogin(false)  // 允许踢出旧会话
                );

        log.info("✅ OAuth2.1授权服务器安全过滤器链配置完成");
        return http.build();
    }

    /**
     * OAuth2.1资源服务器安全过滤器链
     * 处理受保护的API端点，验证JWT令牌
     * <p>
     * 处理的端点:
     * - /auth/** (认证相关API)
     * - /admin/** (管理API)
     * - 其他需要JWT验证的API
     */
    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 配置OAuth2.1资源服务器安全过滤器链");

        http
                // 匹配需要JWT验证的端点
                .securityMatcher(
                        "/auth/validate-token",
                        "/auth/refresh-token",
                        "/admin/**",
                        "/management/**"
                )

                // OAuth2.1资源服务器配置
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // 授权配置
                .authorizeHttpRequests(authorize -> authorize
                        // 管理端点需要管理员权限
                        .requestMatchers("/admin/**", "/management/**")
                        .hasAnyRole("ADMIN")

                        // 其他端点需要认证
                        .anyRequest().authenticated()
                )

                // OAuth2.1资源服务器JWT配置
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )

                        // JWT认证异常处理
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("🔒 JWT认证失败: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"JWT令牌无效或已过期\"}"
                            );
                        })

                        // JWT授权异常处理
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("🚫 JWT授权失败: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"access_denied\",\"message\":\"权限不足\"}"
                            );
                        })
                )

                // 无状态会话
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        log.info("✅ OAuth2.1资源服务器安全过滤器链配置完成");
        return http.build();
    }

    /**
     * 默认安全过滤器链
     * 处理其他所有请求，包括公开API和文档端点
     * <p>
     * 处理的端点:
     * - 公开API (如注册、登录等)
     * - 文档端点 (Swagger, Actuator等)
     * - 静态资源
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 配置默认安全过滤器链");

        http
                // OAuth2.1基础配置
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // 授权配置
                .authorizeHttpRequests(authorize -> authorize
                        // 完全公开的端点
                        .requestMatchers(
                                // 健康检查和监控
                                "/actuator/**",
                                "/health/**",

                                // API文档 - Swagger/OpenAPI
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                // Knife4j文档 - 公开访问
                                "/doc.html",              // Knife4j文档首页
                                "/doc.html/**",           // Knife4j相关资源
                                "/favicon.ico",
                                "/error",

                                // 简单登录页面 - 公开访问
                                "/login",                 // 简单登录页面
                                "/login/**",              // 登录相关资源

                                // 公开API
                                "/auth/register",        // 用户注册
                                "/auth/login",           // 用户登录
                                "/auth/logout",          // 用户登出
                                "/auth/register-and-login"  // 注册并登录
                        ).permitAll()

                        // 其他请求允许访问（由网关统一鉴权）
                        .anyRequest().permitAll()
                )

                // 禁用不需要的认证方式
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 无状态会话
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        log.info("✅ 默认安全过滤器链配置完成");
        return http.build();
    }

    /**
     * CORS配置（全局）
     * OAuth2.1标准推荐的跨域配置
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        log.info("🔧 配置CORS跨域设置");

        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();

        // OAuth2.1 CORS设置
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://localhost:*");
        configuration.addAllowedOriginPattern("http://127.0.0.1:*");
        configuration.addAllowedOriginPattern("https://127.0.0.1:*");

        // 允许的HTTP方法
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");

        // 允许的请求头
        configuration.addAllowedHeader("*");

        // 暴露的响应头（OAuth2.1需要）
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Cache-Control");
        configuration.addExposedHeader("Content-Type");

        // 预检请求缓存时间
        configuration.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("✅ CORS跨域设置配置完成");
        return source;
    }
}

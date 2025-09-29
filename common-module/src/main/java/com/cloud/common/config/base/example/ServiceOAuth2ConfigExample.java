package com.cloud.common.config.base.example;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * 服务级别OAuth2配置示例
 * 展示各个微服务如何继承BaseOAuth2ResourceServerConfig并自定义配置
 * 
 * <p>使用方式：</p>
 * <ol>
 *   <li>继承BaseOAuth2ResourceServerConfig</li>
 *   <li>实现抽象方法定义服务特定的权限规则</li>
 *   <li>可选择性重写方法添加自定义验证器</li>
 *   <li>创建SecurityFilterChain Bean</li>
 * </ol>
 * 
 * @author CloudDevAgent
 * @since 2025-09-26
 */
// @Configuration  // 禁用示例配置以避免与实际服务配置冲突
public class ServiceOAuth2ConfigExample extends BaseOAuth2ResourceServerConfig {

    @Override
    protected void configurePublicPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Actuator健康检查端点
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Knife4j API文档
                .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                // 服务特定的公开API
                .requestMatchers("/api/public/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // 使用统一权限管理的表达式
        String permissionPrefix = getPermissionExpressionPrefix();
        
        if (isUnifiedSecurityEnabled()) {
            // 使用统一权限检查器的表达式
            auth
                    // 管理员专用API
                    .requestMatchers("/api/admin/**")
                    .hasAuthority("SCOPE_admin")
                    .requestMatchers("/api/internal/**")
                    .hasAuthority("SCOPE_internal_api")
                    .requestMatchers("/api/user/**")
                    .hasAuthority("SCOPE_user")
                    .requestMatchers("/api/manage/**")
                    .hasAuthority("SCOPE_manage");
        } else {
            // 使用标准OAuth2 scope权限
            auth
                    .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
                    .requestMatchers("/api/internal/**").hasAuthority("SCOPE_internal_api")  
                    .requestMatchers("/api/user/**").hasAuthority("SCOPE_user")
                    .requestMatchers("/api/manage/**").hasAuthority("SCOPE_manage");
        }
    }

    @Override
    protected void addCustomValidators(List<OAuth2TokenValidator<Jwt>> validators) {
        // 可以添加自定义验证器，例如：
        // validators.add(new JwtBlacklistValidator());
        // validators.add(new CustomJwtValidator());
    }

    @Override
    protected String getServiceName() {
        return "示例服务";
    }
    /**
     * 创建安全过滤器链 Bean示例
     * 注意：实际使用时需要在各服务的配置类中创建此Bean
     */
    // @Bean  // 禁用示例 Bean 以避免与实际服务配置冲突
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SecurityFilterChain filterChain = createSecurityFilterChain(http);
        logConfigurationComplete();
        return filterChain;
    }
}

// ========================================
// 具体服务配置示例
// ========================================

/**
 * 用户服务OAuth2配置示例
 * 注意：实际使用时需要在各服务中创建独立的配置类
 */
// @Configuration  // 禁用示例配置以避免与实际服务配置冲突
class UserServiceOAuth2Config extends BaseOAuth2ResourceServerConfig {

    @Override
    protected void configurePublicPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/user/public/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (isUnifiedSecurityEnabled()) {
            auth
                    .requestMatchers("/user/admin/**")
                    .hasAuthority("SCOPE_admin")
                    .requestMatchers("/user/internal/**")
                    .hasAuthority("SCOPE_internal_api")
                    .requestMatchers("/user/profile/**")
                    .hasAuthority("SCOPE_user");
        } else {
            auth
                    .requestMatchers("/user/admin/**").hasAuthority("SCOPE_admin")
                    .requestMatchers("/user/internal/**").hasAuthority("SCOPE_internal_api")
                    .requestMatchers("/user/profile/**").hasAuthority("SCOPE_user");
        }
    }

    @Override
    protected String getServiceName() {
        return "用户服务";
    }

    // @Bean  // 禁用示例 Bean 以避免与实际服务配置冲突
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        return createSecurityFilterChain(http);
    }
}

/**
 * 订单服务OAuth2配置示例
 * 注意：实际使用时需要在各服务中创建独立的配置类  
 */
// @Configuration  // 禁用示例配置以避免与实际服务配置冲突
class OrderServiceOAuth2Config extends BaseOAuth2ResourceServerConfig {

    @Override
    protected void configurePublicPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (isUnifiedSecurityEnabled()) {
            auth
                    .requestMatchers("/order/admin/**")
                    .hasAuthority("SCOPE_admin")
                    .requestMatchers("/order/internal/**")
                    .hasAuthority("SCOPE_internal_api")
                    .requestMatchers("/order/manage/**")
                    .hasAuthority("SCOPE_order_manage");
        } else {
            auth
                    .requestMatchers("/order/admin/**").hasAuthority("SCOPE_admin")
                    .requestMatchers("/order/internal/**").hasAuthority("SCOPE_internal_api")
                    .requestMatchers("/order/manage/**").hasAuthority("SCOPE_order_manage");
        }
    }

    @Override
    protected String getServiceName() {
        return "订单服务";
    }

    // @Bean  // 禁用示例 Bean 以避免与实际服务配置冲突
    public SecurityFilterChain orderSecurityFilterChain(HttpSecurity http) throws Exception {
        return createSecurityFilterChain(http);
    }
}

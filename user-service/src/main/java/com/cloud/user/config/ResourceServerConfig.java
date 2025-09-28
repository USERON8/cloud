package com.cloud.user.config;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * 用户服务 OAuth2资源服务器配置
 * 继承通用配置，添加用户服务特定的安全配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@Order(101)
public class ResourceServerConfig extends BaseOAuth2ResourceServerConfig {

    private final TokenBlacklistChecker tokenBlacklistChecker;

    public ResourceServerConfig(TokenBlacklistChecker tokenBlacklistChecker) {
        this.tokenBlacklistChecker = tokenBlacklistChecker;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SecurityFilterChain chain = createSecurityFilterChain(http);
        logConfigurationComplete();
        return chain;
    }

    @Override
    protected void configurePublicPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // 公共端点放行
                .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                // 用户注册登录接口放行（这些应该通过auth-service处理）
                .requestMatchers("/user/register", "/user/login").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (isUnifiedSecurityEnabled()) {
            // 使用统一权限管理
            auth
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
                    .requestMatchers("/example/permissions/**").authenticated();
        } else {
            // 回退到标准OAuth2权限
            auth
                    // 内部API统一权限验证
                    .requestMatchers("/user/internal/**").hasAuthority("SCOPE_internal_api")
                    // 用户管理接口
                    .requestMatchers("/user/manage/**").authenticated()
                    // 管理员管理接口
                    .requestMatchers("/admin/manage/**").hasAuthority("ROLE_ADMIN")
                    // 用户相关接口
                    .requestMatchers("/api/user/profile/**").hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write")
                    .requestMatchers("/api/user/address/**").hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write")
                    // 商户相关接口
                    .requestMatchers("/api/user/merchant/**").hasAnyAuthority("SCOPE_read", "SCOPE_write")
                    // 权限示例接口
                    .requestMatchers("/example/permissions/**").authenticated();
        }
    }

    @Override
    protected void addCustomValidators(List<OAuth2TokenValidator<Jwt>> validators) {
        // 添加令牌黑名单验证器
        validators.add(tokenBlacklistChecker);
        log.info("✅ 已添加令牌黑名单验证器");
    }

    @Override
    protected String getServiceName() {
        return "用户服务";
    }
}
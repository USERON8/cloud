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
        auth
                // 内部API统一权限验证 - 修复安全问题
                // 注意：这些接口仅供服务间调用，需要internal_api scope
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
                .requestMatchers("/example/permissions/**").authenticated();
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
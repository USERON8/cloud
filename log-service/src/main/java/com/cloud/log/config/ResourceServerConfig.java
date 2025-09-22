package com.cloud.log.config;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 日志服务 OAuth2资源服务器配置
 * 继承通用配置，添加日志服务特定的安全配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@Order(101)
public class ResourceServerConfig extends BaseOAuth2ResourceServerConfig {

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
                // 公开访问的端点
                .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                // Knife4j和Swagger文档相关路径
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // 内部API需要internal_api scope
                .requestMatchers("/log/internal/**").hasAuthority("SCOPE_internal_api")
                // 日志管理接口需要write scope（管理员权限）
                .requestMatchers("/log/manage/**").hasAnyAuthority("SCOPE_write", "SCOPE_log.write", "ROLE_ADMIN")
                // 日志查询接口需要read scope
                .requestMatchers("/log/query/**").hasAnyAuthority("SCOPE_read", "SCOPE_log.read")
                // API路径配置
                .requestMatchers("/api/log/**").hasAnyAuthority("SCOPE_read", "SCOPE_write", "SCOPE_log.read", "SCOPE_log.write")
                // 其他日志接口需要read scope
                .requestMatchers("/log/**").hasAnyAuthority("SCOPE_read", "SCOPE_log.read");
    }

    @Override
    protected String getServiceName() {
        return "日志服务";
    }
}

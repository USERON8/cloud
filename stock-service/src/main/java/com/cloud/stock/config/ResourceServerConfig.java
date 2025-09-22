package com.cloud.stock.config;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 库存服务 OAuth2资源服务器配置
 * 继承通用配置，添加库存服务特定的安全配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
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
                // Swagger和API文档端点
                .requestMatchers("/doc.html/**", "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // 内部API需要internal_api scope
                .requestMatchers("/api/stock/internal/**").hasAuthority("SCOPE_internal_api")
                // 库存查询接口需要read scope
                .requestMatchers("/api/stock/query/**").hasAnyAuthority("SCOPE_read", "SCOPE_user.read")
                // 库存管理接口需要write scope
                .requestMatchers("/api/stock/manage/**").hasAnyAuthority("SCOPE_write", "SCOPE_user.write");
    }

    @Override
    protected String getServiceName() {
        return "库存服务";
    }
}

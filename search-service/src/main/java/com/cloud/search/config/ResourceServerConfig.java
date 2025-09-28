package com.cloud.search.config;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 搜索服务 OAuth2资源服务器配置
 * 继承通用配置，添加搜索服务特定的安全配置
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
                // 公共端点放行
                .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll();
    }

    @Override
    protected void configureProtectedPaths(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (isUnifiedSecurityEnabled()) {
            // 使用统一权限管理
            auth
                    // 内部API需要internal_api scope
                    .requestMatchers("/search/internal/**")
                    .hasAuthority("SCOPE_internal_api")
                    
                    // 搜索管理接口 - 需要搜索管理权限或管理员权限
                    .requestMatchers("/search/manage/**")
                    .hasAnyAuthority("SCOPE_write", "ROLE_ADMIN")
                    
                    // 搜索查询接口 - 公开可访问，但需要认证
                    .requestMatchers("/search/query/**")
                    .authenticated()
                        
                    // API路径配置 - 需要搜索相关权限
                    .requestMatchers("/api/search/**")
                    .hasAnyAuthority("SCOPE_read", "SCOPE_write", "ROLE_USER", "ROLE_ADMIN")
                        
                    // 其他搜索接口 - 公开可访问（需要认证）
                    .requestMatchers("/search/**")
                    .authenticated();
        } else {
            // 回退到标准OAuth2权限
            auth
                    // 内部API需要internal_api scope
                    .requestMatchers("/search/internal/**").hasAuthority("SCOPE_internal_api")
                    // 搜索管理接口需要write scope
                    .requestMatchers("/search/manage/**").hasAnyAuthority("SCOPE_write", "SCOPE_search.write")
                    // 搜索查询接口需要read scope
                    .requestMatchers("/search/query/**").hasAnyAuthority("SCOPE_read", "SCOPE_search.read")
                    // API路径配置
                    .requestMatchers("/api/search/**").hasAnyAuthority("SCOPE_read", "SCOPE_write", "SCOPE_search.read", "SCOPE_search.write")
                    // 其他搜索接口需要read scope
                    .requestMatchers("/search/**").hasAnyAuthority("SCOPE_read", "SCOPE_search.read");
        }
    }

    @Override
    protected String getServiceName() {
        return "搜索服务";
    }
}

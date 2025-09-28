package com.cloud.order.config;

import com.cloud.common.config.base.BaseOAuth2ResourceServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 订单服务 OAuth2资源服务器配置
 * 继承通用配置，添加订单服务特定的安全配置
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
                    .requestMatchers("/order/internal/**")
                    .hasAuthority("SCOPE_internal_api")
                    
                    // 订单管理接口 - 需要订单管理权限或管理员权限
                    .requestMatchers("/order/manage/**")
                    .hasAnyAuthority("SCOPE_write", "ROLE_ADMIN")
                    
                    // 订单查询接口 - 用户可以查看自己的订单，管理员可以查看所有订单
                    .requestMatchers("/order/query/**")
                    .hasAnyAuthority("SCOPE_read", "SCOPE_order.read", "ROLE_USER", "ROLE_ADMIN")
                        
                    // API路径配置 - 需要订单相关权限
                    .requestMatchers("/api/order/**")
                    .hasAnyAuthority("SCOPE_read", "SCOPE_write", "ROLE_USER", "ROLE_ADMIN");
        } else {
            // 回退到标准OAuth2权限
            auth
                    // 内部API需要internal_api scope
                    .requestMatchers("/order/internal/**").hasAuthority("SCOPE_internal_api")
                    // 订单管理接口需要write scope
                    .requestMatchers("/order/manage/**").hasAnyAuthority("SCOPE_write", "SCOPE_order.write")
                    // 订单查询接口需要read scope
                    .requestMatchers("/order/query/**").hasAnyAuthority("SCOPE_read", "SCOPE_order.read")
                    // API路径配置
                    .requestMatchers("/api/order/**").hasAnyAuthority("SCOPE_read", "SCOPE_write", "SCOPE_order.read", "SCOPE_order.write");
        }
    }

    @Override
    protected String getServiceName() {
        return "订单服务";
    }
}

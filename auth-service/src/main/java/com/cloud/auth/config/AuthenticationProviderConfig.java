package com.cloud.auth.config;

import com.cloud.auth.service.CustomUserDetailsServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 认证提供者配置
 * 严格遵循OAuth2.1标准，配置用户认证相关组件
 * <p>
 * 功能包括:
 * - UserDetailsService配置
 * - AuthenticationProvider配置
 * - AuthenticationManager配置
 * - 认证异常处理
 *
 * @author what's up
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthenticationProviderConfig {

    private final CustomUserDetailsServiceImpl customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * DAO认证提供者配置
     * 使用自定义UserDetailsService和密码编码器
     */
    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        log.info("🔧 配置DAO认证提供者");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        // 设置自定义用户详情服务
        provider.setUserDetailsService(customUserDetailsService);

        // 设置密码编码器
        provider.setPasswordEncoder(passwordEncoder);

        // OAuth2.1安全配置
        // 注：Spring Security 6.x 会自动处理账户状态检查，无需自定义
        // DaoAuthenticationProvider 默认已经包含完整的账户状态检查逻辑

        log.info("✅ DAO认证提供者配置完成");
        return provider;
    }

    /**
     * 认证管理器配置
     * OAuth2.1授权服务器需要认证管理器来验证用户凭证
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.info("🔧 配置认证管理器");

        AuthenticationManager authenticationManager =
                authenticationConfiguration.getAuthenticationManager();

        log.info("✅ 认证管理器配置完成");
        return authenticationManager;
    }

    // 注释掉不再使用的AuthenticationManagerBuilder配置
    // 在Spring Security 6.x中推荐使用AuthenticationConfiguration获取AuthenticationManager

    // 注：内部静态类直接使用 @Component 注解，而不是通过 @Bean 创建

    /**
     * 用户详情服务验证器实现
     */
    @Component
    @Slf4j
    public static class UserDetailsServiceValidator {
        private final CustomUserDetailsServiceImpl userDetailsService;

        public UserDetailsServiceValidator(CustomUserDetailsServiceImpl userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        /**
         * 验证UserDetailsService配置
         */
        @PostConstruct
        private void validateConfiguration() {
            log.info("🔍 验证用户详情服务配置");

            try {
                // 验证服务是否正常工作
                if (userDetailsService == null) {
                    throw new IllegalStateException("CustomUserDetailsService未正确注入");
                }

                log.info("✅ 用户详情服务配置验证通过");

            } catch (Exception e) {
                log.error("🚨 用户详情服务配置验证失败", e);
                throw new IllegalStateException("用户详情服务配置验证失败", e);
            }
        }

        /**
         * 获取用户详情服务统计信息
         */
        public String getServiceInfo() {
            return String.format("UserDetailsService: %s, 状态: 正常",
                    userDetailsService.getClass().getSimpleName());
        }
    }

    /**
     * 认证事件监听器实现
     */
    @Component
    @Slf4j
    public static class AuthenticationEventListener {

        @org.springframework.context.event.EventListener
        public void handleAuthenticationSuccess(
                org.springframework.security.authentication.event.AuthenticationSuccessEvent event) {

            String username = event.getAuthentication().getName();
            String authorities = event.getAuthentication().getAuthorities().toString();

            log.info("✅ 用户认证成功: username={}, authorities={}", username, authorities);

            // 可以在这里添加认证成功后的业务逻辑
            // 如：更新最后登录时间、记录登录日志等
        }

        @org.springframework.context.event.EventListener
        public void handleAuthenticationFailure(
                org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent event) {

            String username = event.getAuthentication().getName();
            String exceptionMessage = event.getException().getMessage();

            log.warn("❌ 用户认证失败: username={}, reason={}", username, exceptionMessage);

            // 可以在这里添加认证失败后的业务逻辑
            // 如：记录失败次数、实现账户锁定等
        }

        @org.springframework.context.event.EventListener
        public void handleBadCredentials(
                org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent event) {

            String username = event.getAuthentication().getName();

            log.warn("🔑 用户密码错误: username={}", username);

            // 可以在这里实现密码错误次数统计和账户保护逻辑
        }

        @org.springframework.context.event.EventListener
        public void handleUserNotFound(
                org.springframework.security.authentication.event.AuthenticationFailureCredentialsExpiredEvent event) {

            String username = event.getAuthentication().getName();

            log.warn("⏰ 用户凭证已过期: username={}", username);

            // 可以在这里实现凭证过期处理逻辑
        }
    }
}
package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2资源服务器配置属性类
 * 支持JWT和安全配置的外部化
 *
 * @author what's up
 * @date 2025-01-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.security.oauth2.resource-server")
public class OAuth2ResourceServerProperties {

    /**
     * 是否启用资源服务器
     */
    private boolean enabled = true;

    /**
     * JWT配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * 权限配置
     */
    private AuthorityConfig authority = new AuthorityConfig();

    /**
     * 公开路径配置
     */
    private List<String> publicPaths = new ArrayList<>();

    /**
     * CORS配置
     */
    private CorsConfig cors = new CorsConfig();

    /**
     * 会话配置
     */
    private SessionConfig session = new SessionConfig();

    @Data
    public static class JwtConfig {
        /**
         * JWT验证器缓存时长（分钟）
         */
        private int cacheMinutes = 30;

        /**
         * 是否启用JWT黑名单检查
         */
        private boolean blacklistEnabled = false;

        /**
         * JWT时钟偏移容忍度（秒）
         */
        private int clockSkewSeconds = 60;

        /**
         * 是否验证audience
         */
        private boolean validateAudience = false;

        /**
         * 期望的audience列表
         */
        private List<String> expectedAudiences = new ArrayList<>();

        /**
         * 是否验证not before
         */
        private boolean validateNotBefore = true;

        /**
         * JWT最大有效时长（小时）
         */
        private int maxValidityHours = 24;
    }

    @Data
    public static class AuthorityConfig {
        /**
         * 权限前缀
         */
        private String prefix = "SCOPE_";

        /**
         * 权限声明名称
         */
        private String claimName = "scope";

        /**
         * 是否启用角色映射
         */
        private boolean roleMapping = false;

        /**
         * 角色前缀
         */
        private String rolePrefix = "ROLE_";

        /**
         * 是否从多个声明中提取权限
         */
        private boolean multiClaimExtraction = false;

        /**
         * 额外的权限声明名称列表
         */
        private List<String> additionalClaimNames = new ArrayList<>();
    }

    @Data
    public static class CorsConfig {
        /**
         * 是否启用CORS
         */
        private boolean enabled = true;

        /**
         * 允许的源
         */
        private List<String> allowedOrigins = List.of("*");

        /**
         * 允许的方法
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

        /**
         * 允许的请求头
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * 暴露的响应头
         */
        private List<String> exposedHeaders = new ArrayList<>();

        /**
         * 是否允许凭证
         */
        private boolean allowCredentials = true;

        /**
         * 预检请求缓存时间（秒）
         */
        private long maxAgeSeconds = 3600;
    }

    @Data
    public static class SessionConfig {
        /**
         * 会话创建策略
         */
        private String creationPolicy = "STATELESS";

        /**
         * 是否启用会话固定保护
         */
        private boolean sessionFixationProtection = false;

        /**
         * 最大会话数
         */
        private int maximumSessions = 1;

        /**
         * 是否阻止新会话创建
         */
        private boolean maxSessionsPreventsLogin = false;
    }
}


package com.cloud.auth.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 认证服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 认证数据需要保证强一致性和安全性，支持会话管理
 * 使用会话专用配置，支持令牌管理
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class AuthRedisConfig extends BaseRedisConfig {

    /**
     * 认证服务专用的RedisTemplate配置
     * 使用会话专用配置，支持事务（用于令牌管理）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化认证服务Redis配置");
        return RedisConfigFactory.createSessionRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "auth";
    }

    /**
     * 认证服务缓存过期时间配置
     * 认证数据根据安全性要求设置不同的缓存时间
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "accessToken":
                return 1800L; // 30分钟（访问令牌）
            case "refreshToken":
                return 7200L; // 2小时（刷新令牌）
            case "authCode":
                return 300L;  // 5分钟（授权码）
            case "userSession":
                return 3600L; // 1小时（用户会话）
            case "loginAttempt":
                return 900L;  // 15分钟（登录尝试）
            case "captcha":
                return 300L;  // 5分钟（验证码）
            case "smsCode":
                return 300L;  // 5分钟（短信验证码）
            case "emailCode":
                return 600L;  // 10分钟（邮箱验证码）
            case "clientInfo":
                return 86400L; // 24小时（客户端信息）
            default:
                return 1800L; // 默认30分钟
        }
    }

    /**
     * 认证服务需要事务支持（用于令牌管理）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}

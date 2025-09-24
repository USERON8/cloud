package com.cloud.user.config;

import com.cloud.common.config.BaseLocalCacheConfig;
import com.cloud.common.config.MultiLevelCacheConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

/**
 * 用户服务本地缓存配置类
 * 提供基于Caffeine的本地缓存配置，用于实现多级缓存中的L1缓存层
 * L1: 本地缓存 (Caffeine) + L2: 分布式缓存 (Redis)
 * <p>
 * 用户服务特点：用户信息相对稳定，访问频繁
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableCaching
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class UserLocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 用户服务专用的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对用户服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Bean
    @Primary
    public CacheManager localCacheManager() {
        log.info("初始化用户服务本地缓存管理器");
        return MultiLevelCacheConfigFactory.createUserServiceCacheManager();
    }

    @Override
    protected String getServiceName() {
        return "user-service";
    }

    @Override
    protected String[] getCacheNames() {
        return new String[]{
                "userCache",            // 用户基础信息缓存
                "userProfileCache",     // 用户详细信息缓存
                "userAddressCache",     // 用户地址缓存
                "userStatsCache",       // 用户统计信息缓存
                "userPermissionCache",  // 用户权限缓存
                "userTokenCache"        // 用户令牌缓存
        };
    }
}

package com.cloud.user.config;

import com.cloud.common.config.BaseLocalCacheConfig;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务本地缓存配置类
 * 提供基于Caffeine的本地缓存配置，用于实现多级缓存中的L1缓存层
 * L1: 本地缓存 (Caffeine) + L2: 分布式缓存 (Redis)
 * <p>
 * 支持传统的Spring Cache注解(@Cacheable等)和自定义的多级缓存注解(@MultiLevelCacheable等)
 *
 * @author what's up
 */
@Configuration
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class LocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 重写父类的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对用户服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Override
    @Primary
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 使用父类提供的工具方法创建Caffeine配置
        // 针对用户服务的缓存特点：高频访问，长时间保存
        cacheManager.setCaffeine(buildCaffeineSpec(
                200,    // 初始容量
                2000L,  // 最大缓存条目数
                60L,    // 写入后60分钟过期
                30L,    // 访问后30分钟过期
                TimeUnit.MINUTES
        ));

        // 预定义缓存名称，提高性能
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "userCache",           // 用户信息缓存
                "userAddressCache",    // 用户地址缓存
                "merchantCache",       // 商户信息缓存
                "merchantAuthCache",   // 商户认证信息缓存
                "adminCache",          // 管理员信息缓存
                "userStatsCache"       // 用户统计信息缓存
        ));

        return cacheManager;
    }
}

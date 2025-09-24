package com.cloud.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Arrays;

/**
 * 多级缓存配置工厂类
 * 提供各种预定义的多级缓存配置模板
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
public class MultiLevelCacheConfigFactory {

    /**
     * 创建用户服务多级缓存配置
     * L1: Caffeine本地缓存 + L2: Redis分布式缓存
     *
     * @return CacheManager
     */
    public static CacheManager createUserServiceCacheManager() {
        log.info("创建用户服务多级缓存配置");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 用户服务缓存特点：用户信息相对稳定，访问频繁
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(200)           // 初始容量
                .maximumSize(2000L)             // 最大缓存条目数
                .expireAfterWrite(Duration.ofMinutes(30))   // 写入后30分钟过期
                .expireAfterAccess(Duration.ofMinutes(15))  // 访问后15分钟过期
                .recordStats();                 // 开启统计

        cacheManager.setCaffeine(caffeine);

        // 预定义缓存名称
        cacheManager.setCacheNames(Arrays.asList(
                "userCache",            // 用户基础信息缓存
                "userProfileCache",     // 用户详细信息缓存
                "userAddressCache",     // 用户地址缓存
                "userStatsCache",       // 用户统计信息缓存
                "userPermissionCache",  // 用户权限缓存
                "userTokenCache"        // 用户令牌缓存
        ));

        return cacheManager;
    }

    /**
     * 创建商品服务多级缓存配置
     * L1: Caffeine本地缓存 + L2: Redis分布式缓存
     *
     * @return CacheManager
     */
    public static CacheManager createProductServiceCacheManager() {
        log.info("创建商品服务多级缓存配置");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 商品服务缓存特点：商品信息相对稳定，查询频繁
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(150)           // 初始容量
                .maximumSize(1500L)             // 最大缓存条目数
                .expireAfterWrite(Duration.ofMinutes(45))   // 写入后45分钟过期
                .expireAfterAccess(Duration.ofMinutes(20))  // 访问后20分钟过期
                .recordStats();                 // 开启统计

        cacheManager.setCaffeine(caffeine);

        // 预定义缓存名称
        cacheManager.setCacheNames(Arrays.asList(
                "productCache",         // 商品基础信息缓存
                "productListCache",     // 商品列表查询缓存
                "productStatsCache",    // 商品统计信息缓存
                "shopCache",            // 店铺信息缓存
                "shopListCache",        // 店铺列表查询缓存
                "categoryCache",        // 商品分类缓存
                "categoryTreeCache"     // 分类树形结构缓存
        ));

        return cacheManager;
    }

    /**
     * 创建搜索服务多级缓存配置
     * L1: Caffeine本地缓存 + L2: Redis分布式缓存
     *
     * @return CacheManager
     */
    public static CacheManager createSearchServiceCacheManager() {
        log.info("创建搜索服务多级缓存配置");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 搜索服务缓存特点：搜索结果变化较快，但热门搜索相对稳定
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(300)           // 初始容量
                .maximumSize(3000L)             // 最大缓存条目数
                .expireAfterWrite(Duration.ofMinutes(20))   // 写入后20分钟过期
                .expireAfterAccess(Duration.ofMinutes(10))  // 访问后10分钟过期
                .recordStats();                 // 开启统计

        cacheManager.setCaffeine(caffeine);

        // 预定义缓存名称
        cacheManager.setCacheNames(Arrays.asList(
                "productSearchCache",   // 商品搜索结果缓存
                "searchSuggestionCache", // 搜索建议缓存
                "hotSearchCache",       // 热门搜索缓存
                "searchStatsCache",     // 搜索统计缓存
                "filterCache",          // 搜索过滤器缓存
                "aggregationCache"      // 聚合查询结果缓存
        ));

        return cacheManager;
    }

    /**
     * 创建自定义多级缓存配置
     *
     * @param builder 配置构建器
     * @return CacheManager
     */
    public static CacheManager createCustomCacheManager(CacheManagerBuilder builder) {
        log.info("创建自定义多级缓存配置");
        return builder.build();
    }

    /**
     * 创建Redis配置（用于L2缓存）
     *
     * @param connectionFactory Redis连接工厂
     * @param servicePrefix     服务前缀
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createMultiLevelRedisTemplate(
            RedisConnectionFactory connectionFactory, String servicePrefix) {
        log.info("创建多级缓存Redis配置，服务前缀: {}", servicePrefix);
        return RedisConfigFactory.createCacheRedisTemplate(connectionFactory);
    }

    /**
     * 缓存管理器配置构建器
     */
    public static class CacheManagerBuilder {
        private int initialCapacity = 100;
        private long maximumSize = 1000L;
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        private Duration expireAfterAccess = Duration.ofMinutes(15);
        private boolean recordStats = true;
        private String[] cacheNames = new String[0];

        public CacheManagerBuilder initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        public CacheManagerBuilder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public CacheManagerBuilder expireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
            return this;
        }

        public CacheManagerBuilder expireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
            return this;
        }

        public CacheManagerBuilder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public CacheManagerBuilder cacheNames(String... cacheNames) {
            this.cacheNames = cacheNames;
            return this;
        }

        public CacheManager build() {
            CaffeineCacheManager cacheManager = new CaffeineCacheManager();

            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .initialCapacity(initialCapacity)
                    .maximumSize(maximumSize)
                    .expireAfterWrite(expireAfterWrite)
                    .expireAfterAccess(expireAfterAccess);

            if (recordStats) {
                caffeine.recordStats();
            }

            cacheManager.setCaffeine(caffeine);

            if (cacheNames.length > 0) {
                cacheManager.setCacheNames(Arrays.asList(cacheNames));
            }

            return cacheManager;
        }
    }
}

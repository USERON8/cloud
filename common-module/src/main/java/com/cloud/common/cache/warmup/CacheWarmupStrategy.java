package com.cloud.common.cache.warmup;

/**
 * 缓存预热策略接口
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
public interface CacheWarmupStrategy {

    /**
     * 执行缓存预热
     *
     * @param cacheManager 缓存管理器
     * @return 预热的数据项数量
     */
    int warmup(org.springframework.cache.CacheManager cacheManager);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
}

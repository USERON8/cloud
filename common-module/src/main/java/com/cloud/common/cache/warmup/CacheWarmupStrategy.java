package com.cloud.common.cache.warmup;







public interface CacheWarmupStrategy {

    





    int warmup(org.springframework.cache.CacheManager cacheManager);

    




    String getStrategyName();
}

package com.cloud.search.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存组合注解
 * 类似于Spring的@Caching，用于在同一个方法上应用多个缓存操作
 * <p>
 * 使用场景：
 * 1. 同时操作多个缓存名称的缓存
 * 2. 同时删除多个不同key的缓存项
 * 3. 复杂的缓存更新场景
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCaching {

    /**
     * 多个缓存查询操作
     */
    MultiLevelCacheable[] cacheable() default {};

    /**
     * 多个缓存更新操作
     */
    MultiLevelCachePut[] put() default {};

    /**
     * 多个缓存删除操作
     */
    MultiLevelCacheEvict[] evict() default {};
}

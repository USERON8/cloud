package com.cloud.common.cache.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存组合注解
 *
 * @author what's up
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCaching {

    /**
     * 缓存查询操作
     */
    MultiLevelCacheable[] cacheable() default {};

    /**
     * 缓存更新操作
     */
    MultiLevelCachePut[] put() default {};

    /**
     * 缓存清除操作
     */
    MultiLevelCacheEvict[] evict() default {};
}

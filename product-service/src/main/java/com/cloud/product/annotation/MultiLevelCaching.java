package com.cloud.product.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存组合注觢
 * 可以组合使用MultiLevelCacheable、MultiLevelCachePut、MultiLevelCacheEvict
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
     * 缓存删除操作
     */
    MultiLevelCacheEvict[] evict() default {};
}

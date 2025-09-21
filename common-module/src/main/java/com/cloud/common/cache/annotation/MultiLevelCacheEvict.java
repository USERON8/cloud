package com.cloud.common.cache.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存清除注解
 *
 * @author what's up
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCacheEvict {

    /**
     * 缓存名称数组
     */
    String[] value() default {};

    /**
     * 缓存key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 是否清除所有条目
     */
    boolean allEntries() default false;

    /**
     * 条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 是否在方法执行前清除缓存
     */
    boolean beforeInvocation() default false;

    /**
     * 是否启用本地缓存
     */
    boolean enableLocalCache() default true;
}

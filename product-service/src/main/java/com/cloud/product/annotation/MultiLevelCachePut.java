package com.cloud.product.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存更新注解
 * 类似于Spring的@CachePut，但会同时更新L1和L2缓存
 * 总是执行方法并更新缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCachePut {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     * 支持方法参数引用，如：#id, #result.id
     */
    String key() default "";

    /**
     * 缓存条件SpEL表达式
     * 当条件为true时才进行缓存操作
     */
    String condition() default "";

    /**
     * 缓存拒绝条件SpEL表达式
     * 当条件为true时不缓存返回值
     */
    String unless() default "";

    /**
     * Redis缓存过期时间
     */
    long expire() default 3600;

    /**
     * 过期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用本地缓存
     */
    boolean enableLocalCache() default true;
}

package com.cloud.stock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存查询注解
 * 类似于Spring的@Cacheable，但只使用Redis缓存
 * 保证缓存一致性
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCacheable {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     * 支持方法参数引用，如：#id, #stock.productId
     */
    String key() default "";

    /**
     * 缓存条件SpEL表达式
     * 当条件为true时才进行缓存操作
     */
    String condition() default "";

    /**
     * 缓存拒绝条件SpEL表达式
     * 当条件为true时不缓存返回值（但仍会查询缓存）
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
}

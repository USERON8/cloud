package com.cloud.search.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存查询注解
 * 类似于Spring的@Cacheable，但提供L1(本地缓存)+L2(Redis)的两级缓存支持
 * <p>
 * 访问顺序：L1 Cache(Caffeine) -> L2 Cache(Redis) -> Elasticsearch
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCacheable {

    /**
     * 缓存名称
     * 用于区分不同类型的缓存数据
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     * 支持方法参数引用，如：#keyword, #searchRequest.keyword
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
     * 本地缓存的过期时间在LocalCacheConfig中统一配置
     */
    long expire() default 1800;

    /**
     * 过期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用本地缓存
     * 默认为true，某些场景下可能只需要Redis缓存
     */
    boolean enableLocalCache() default true;
}

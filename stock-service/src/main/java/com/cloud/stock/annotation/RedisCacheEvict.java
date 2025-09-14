package com.cloud.stock.annotation;

import java.lang.annotation.*;

/**
 * Redis缓存删除注解
 * 类似于Spring的@CacheEvict，但只删除Redis缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCacheEvict {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     */
    String key() default "";

    /**
     * 缓存删除条件SpEL表达式
     */
    String condition() default "";

    /**
     * 是否删除缓存名称下的所有缓存项
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前删除缓存
     */
    boolean beforeInvocation() default false;
}

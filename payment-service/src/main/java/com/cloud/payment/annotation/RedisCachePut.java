package com.cloud.payment.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存更新注解
 * 类似于Spring的@CachePut，但只使用Redis缓存
 * 总是执行方法并更新缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCachePut {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     */
    String key() default "";

    /**
     * 缓存条件SpEL表达式
     */
    String condition() default "";

    /**
     * 缓存拒绝条件SpEL表达式
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

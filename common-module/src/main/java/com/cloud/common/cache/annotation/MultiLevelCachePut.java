package com.cloud.common.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存更新注解
 * 
 * @author what's up
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCachePut {
    
    /**
     * 缓存名称
     */
    String value() default "";
    
    /**
     * 缓存key，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 过期时间
     */
    long expire() default 60;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * 条件，支持SpEL表达式
     */
    String condition() default "";
    
    /**
     * 排除条件，支持SpEL表达式
     */
    String unless() default "";
    
    /**
     * 是否启用本地缓存
     */
    boolean enableLocalCache() default true;
}

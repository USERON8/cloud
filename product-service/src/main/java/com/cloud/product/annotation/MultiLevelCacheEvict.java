package com.cloud.product.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存删除注解
 * 类似于Spring的@CacheEvict，但会同时删除L1和L2缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCacheEvict {

    /**
     * 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的SpEL表达式
     * 支持方法参数引用，如：#id, #product.name
     */
    String key() default "";

    /**
     * 缓存删除条件SpEL表达式
     * 当条件为true时才进行删除操作
     */
    String condition() default "";

    /**
     * 是否删除缓存名称下的所有缓存项
     * 为true时会清空整个cacheName下的所有数据
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前删除缓存
     * 为true时在方法执行前删除，为false时在方法执行后删除
     */
    boolean beforeInvocation() default false;
}

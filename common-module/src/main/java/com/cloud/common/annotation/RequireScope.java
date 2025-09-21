package com.cloud.common.annotation;

import java.lang.annotation.*;

/**
 * 权限范围校验注解
 * 用于方法或类级别的OAuth2权限范围校验
 * 
 * @author what's up
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireScope {
    
    /**
     * 必需的权限范围
     * 支持单个或多个权限范围
     * 
     * @return 权限范围数组
     */
    String[] value();
    
    /**
     * 权限检查模式
     * 
     * @return 权限模式
     */
    ScopeMode mode() default ScopeMode.ANY;
    
    /**
     * 自定义错误消息
     * 
     * @return 错误消息
     */
    String message() default "";
    
    /**
     * 权限模式枚举
     */
    enum ScopeMode {
        /**
         * 拥有任意一个权限即可
         */
        ANY,
        
        /**
         * 必须拥有所有权限
         */
        ALL
    }
}

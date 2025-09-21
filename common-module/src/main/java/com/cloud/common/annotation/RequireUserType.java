package com.cloud.common.annotation;

import java.lang.annotation.*;

/**
 * 用户类型校验注解
 * 用于方法或类级别的用户类型校验
 * 
 * @author what's up
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireUserType {
    
    /**
     * 允许的用户类型
     * 支持单个或多个用户类型
     * 
     * @return 用户类型数组
     */
    UserType[] value();
    
    /**
     * 自定义错误消息
     * 
     * @return 错误消息
     */
    String message() default "";
    
    /**
     * 用户类型枚举
     */
    enum UserType {
        /**
         * 普通用户
         */
        USER,
        
        /**
         * 商户
         */
        MERCHANT,
        
        /**
         * 管理员
         */
        ADMIN
    }
}

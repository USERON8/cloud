package com.cloud.common.annotation;

import java.lang.annotation.*;

/**
 * 认证校验注解
 * 用于校验当前请求是否已认证
 * 
 * @author what's up
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuthentication {
    
    /**
     * 自定义错误消息
     * 
     * @return 错误消息
     */
    String message() default "用户未认证，请先登录";
}

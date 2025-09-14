package com.cloud.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 * 用于在方法上标记需要的权限，配合PermissionChecker使用
 *
 * @author what's up
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 需要的权限标识
     */
    String value() default "";

    /**
     * 需要的权限标识数组
     */
    String[] permissions() default {};
}
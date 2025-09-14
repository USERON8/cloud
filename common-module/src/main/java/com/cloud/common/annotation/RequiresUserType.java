package com.cloud.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户类型检查注解
 * 用于在方法上标记需要的用户类型，配合PermissionChecker使用
 *
 * @author what's up
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresUserType {

    /**
     * 需要的用户类型
     * 可选值: USER, MERCHANT, ADMIN
     */
    String value() default "";

    /**
     * 需要的用户类型数组
     * 可选值: USER, MERCHANT, ADMIN
     */
    String[] userTypes() default {};
}
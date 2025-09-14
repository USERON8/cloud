package com.cloud.common.aspect;

import com.cloud.common.annotation.RequiresPermission;
import com.cloud.common.config.PermissionChecker;
import com.cloud.common.exception.PermissionException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限检查切面
 * 自动处理@RequiresPermission注解，实现权限的统一验证
 *
 * @author what's up
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private final PermissionChecker permissionChecker;

    public PermissionAspect(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    /**
     * 权限检查环绕通知
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.cloud.common.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresPermission requiresPermission = method.getAnnotation(RequiresPermission.class);

        // 检查单个权限
        String permission = requiresPermission.value();
        if (!permission.isEmpty()) {
            permissionChecker.assertPermission(permission);
        }

        // 检查多个权限
        String[] permissions = requiresPermission.permissions();
        if (permissions.length > 0) {
            boolean hasAnyPermission = false;
            PermissionException lastException = null;

            for (String perm : permissions) {
                try {
                    permissionChecker.assertPermission(perm);
                    hasAnyPermission = true;
                    break;
                } catch (PermissionException e) {
                    lastException = e;
                }
            }

            if (!hasAnyPermission && lastException != null) {
                throw lastException;
            }
        }

        // 权限检查通过，执行原方法
        return joinPoint.proceed();
    }
}
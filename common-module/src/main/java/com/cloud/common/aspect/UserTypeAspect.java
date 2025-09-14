package com.cloud.common.aspect;

import com.cloud.common.annotation.RequiresUserType;
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
 * 用户类型检查切面
 * 自动处理@RequiresUserType注解，实现用户类型的统一验证
 */
@Slf4j
@Aspect
@Component
public class UserTypeAspect {

    private final PermissionChecker permissionChecker;

    public UserTypeAspect(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    /**
     * 用户类型检查环绕通知
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.cloud.common.annotation.RequiresUserType)")
    public Object checkUserType(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresUserType requiresUserType = method.getAnnotation(RequiresUserType.class);

        // 检查单个用户类型
        String userType = requiresUserType.value();
        if (!userType.isEmpty()) {
            permissionChecker.assertUserType(userType);
        }

        // 检查多个用户类型
        String[] userTypes = requiresUserType.userTypes();
        if (userTypes.length > 0) {
            boolean hasAnyUserType = false;
            PermissionException lastException = null;

            for (String type : userTypes) {
                try {
                    permissionChecker.assertUserType(type);
                    hasAnyUserType = true;
                    break;
                } catch (PermissionException e) {
                    lastException = e;
                }
            }

            if (!hasAnyUserType && lastException != null) {
                throw lastException;
            }
        }

        // 用户类型检查通过，执行原方法
        return joinPoint.proceed();
    }
}
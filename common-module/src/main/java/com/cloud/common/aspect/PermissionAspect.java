package com.cloud.common.aspect;

import com.cloud.common.annotation.RequireAuthentication;
import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.security.PermissionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面
 * 处理权限相关注解，执行权限检查
 * 
 * @author what's up
 */
@Slf4j
@Aspect
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionAspect {
    
    private final PermissionManager permissionManager;
    
    /**
     * 处理@RequireAuthentication注解
     * 
     * @param joinPoint 切入点
     * @param annotation 注解信息
     */
    @Before("@annotation(annotation) || @within(annotation)")
    public void checkAuthentication(JoinPoint joinPoint, RequireAuthentication annotation) {
        log.debug("执行认证检查，方法: {}", joinPoint.getSignature().toShortString());
        
        String customMessage = annotation.message();
        permissionManager.checkAuthentication(customMessage);
        
        log.debug("认证检查通过: {}", joinPoint.getSignature().toShortString());
    }
    
    /**
     * 处理@RequireScope注解
     * 
     * @param joinPoint 切入点
     * @param annotation 注解信息
     */
    @Before("@annotation(annotation) || @within(annotation)")
    public void checkScope(JoinPoint joinPoint, RequireScope annotation) {
        log.debug("执行权限范围检查，方法: {}, 需要权限: {}, 模式: {}", 
            joinPoint.getSignature().toShortString(), 
            java.util.Arrays.toString(annotation.value()), 
            annotation.mode());
        
        String[] requiredScopes = annotation.value();
        RequireScope.ScopeMode mode = annotation.mode();
        String customMessage = annotation.message();
        
        permissionManager.checkScope(requiredScopes, mode, customMessage);
        
        log.debug("权限范围检查通过: {}", joinPoint.getSignature().toShortString());
    }
    
    /**
     * 处理@RequireUserType注解
     * 
     * @param joinPoint 切入点
     * @param annotation 注解信息
     */
    @Before("@annotation(annotation) || @within(annotation)")
    public void checkUserType(JoinPoint joinPoint, RequireUserType annotation) {
        log.debug("执行用户类型检查，方法: {}, 允许类型: {}", 
            joinPoint.getSignature().toShortString(), 
            java.util.Arrays.toString(annotation.value()));
        
        RequireUserType.UserType[] allowedTypes = annotation.value();
        String customMessage = annotation.message();
        
        permissionManager.checkUserType(allowedTypes, customMessage);
        
        log.debug("用户类型检查通过: {}", joinPoint.getSignature().toShortString());
    }
    
    /**
     * 获取方法级别的注解
     * 如果方法上没有注解，则查找类级别的注解
     * 
     * @param joinPoint 切入点
     * @param annotationType 注解类型
     * @param <T> 注解类型
     * @return 注解实例，如果不存在则返回null
     */
    @SuppressWarnings("unused")
    private <T extends java.lang.annotation.Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationType) {
        try {
            Method method = getMethod(joinPoint);
            T annotation = method.getAnnotation(annotationType);
            
            if (annotation == null) {
                // 如果方法上没有，查找类级别的注解
                annotation = joinPoint.getTarget().getClass().getAnnotation(annotationType);
            }
            
            return annotation;
        } catch (Exception e) {
            log.warn("获取注解失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取切入点对应的方法
     * 
     * @param joinPoint 切入点
     * @return 方法对象
     * @throws NoSuchMethodException 方法不存在异常
     */
    private Method getMethod(JoinPoint joinPoint) throws NoSuchMethodException {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Class<?>[] parameterTypes = new Class[args.length];
        
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        
        return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
    }
}

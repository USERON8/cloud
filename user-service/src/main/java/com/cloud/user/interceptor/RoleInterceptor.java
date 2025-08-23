package com.cloud.user.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String userType = request.getHeader("X-User-Type");
        String userRoles = request.getHeader("X-User-Roles");

        // 示例：仅允许admin访问用户管理接口
        if (request.getRequestURI().contains("/admin")) {
            // 检查用户是否具有ADMIN角色
            if (userRoles != null && userRoles.contains("ADMIN")) {
                return true;
            }
            
            // 兼容旧的userType检查
            if ("admin".equals(userType)) {
                return true;
            }
            
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        
        // 其他接口默认允许访问
        return true;
    }
}
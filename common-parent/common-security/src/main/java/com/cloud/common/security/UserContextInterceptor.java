package com.cloud.common.security;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.context.UserContext;
import com.cloud.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String userId = request.getHeader(GatewayIdentityHeaders.USER_ID);
    if (StrUtil.isNotBlank(userId)) {
      UserContext context =
          UserContext.builder()
              .userId(Long.valueOf(userId))
              .username(request.getHeader(GatewayIdentityHeaders.USERNAME))
              .nickname(request.getHeader(GatewayIdentityHeaders.USER_NICKNAME))
              .status(parseInt(request.getHeader(GatewayIdentityHeaders.USER_STATUS)))
              .clientId(request.getHeader(GatewayIdentityHeaders.CLIENT_ID))
              .subject(request.getHeader(GatewayIdentityHeaders.SUBJECT))
              .roles(parseSet(request.getHeader(GatewayIdentityHeaders.USER_ROLES)))
              .permissions(parseSet(request.getHeader(GatewayIdentityHeaders.USER_PERMISSIONS)))
              .scopes(parseSet(request.getHeader(GatewayIdentityHeaders.USER_SCOPES)))
              .build();
      UserContextHolder.setContext(context);
    }
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    UserContextHolder.clear();
  }

  private Integer parseInt(String val) {
    try {
      return StrUtil.isNotBlank(val) ? Integer.parseInt(val) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Set<String> parseSet(String val) {
    if (StrUtil.isBlank(val)) {
      return Collections.emptySet();
    }
    return new HashSet<>(Arrays.asList(val.split(",")));
  }
}

package com.cloud.common.context;

import java.util.Optional;

public final class UserContextHolder {

  private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

  private UserContextHolder() {}

  public static void setContext(UserContext context) {
    CONTEXT_HOLDER.set(context);
  }

  public static UserContext getContext() {
    return CONTEXT_HOLDER.get();
  }

  public static Optional<UserContext> getContextOptional() {
    return Optional.ofNullable(CONTEXT_HOLDER.get());
  }

  public static Long getUserId() {
    return getContextOptional().map(UserContext::getUserId).orElse(null);
  }

  public static String getUsername() {
    return getContextOptional().map(UserContext::getUsername).orElse(null);
  }

  public static void clear() {
    CONTEXT_HOLDER.remove();
  }
}

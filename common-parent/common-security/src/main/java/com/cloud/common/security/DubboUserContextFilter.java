package com.cloud.common.security;

import com.cloud.common.context.UserContext;
import com.cloud.common.context.UserContextHolder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(
    group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER},
    order = -10000)
public class DubboUserContextFilter implements Filter {

  private static final String KEY_USER_ID = "X-User-Id";
  private static final String KEY_USERNAME = "X-Username";
  private static final String KEY_ROLES = "X-User-Roles";

  @Override
  public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    String side = invoker.getUrl().getSide();

    if (CommonConstants.CONSUMER.equals(side)) {
      // Consumer side: put context into RpcContext
      UserContextHolder.getContextOptional()
          .ifPresent(
              context -> {
                RpcContext.getServiceContext()
                    .setAttachment(KEY_USER_ID, String.valueOf(context.getUserId()));
                RpcContext.getServiceContext().setAttachment(KEY_USERNAME, context.getUsername());
                if (context.getRoles() != null && !context.getRoles().isEmpty()) {
                  RpcContext.getServiceContext()
                      .setAttachment(KEY_ROLES, String.join(",", context.getRoles()));
                }
              });
    } else {
      // Provider side: extract context from RpcContext and set to UserContextHolder
      String userId = RpcContext.getServiceContext().getAttachment(KEY_USER_ID);
      if (userId != null) {
        UserContext context =
            UserContext.builder()
                .userId(Long.valueOf(userId))
                .username(RpcContext.getServiceContext().getAttachment(KEY_USERNAME))
                .roles(parseSet(RpcContext.getServiceContext().getAttachment(KEY_ROLES)))
                .build();
        UserContextHolder.setContext(context);
      }
    }

    try {
      return invoker.invoke(invocation);
    } finally {
      if (CommonConstants.PROVIDER.equals(side)) {
        UserContextHolder.clear();
      }
    }
  }

  private Set<String> parseSet(String val) {
    if (val == null || val.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(java.util.Arrays.asList(val.split(",")));
  }
}

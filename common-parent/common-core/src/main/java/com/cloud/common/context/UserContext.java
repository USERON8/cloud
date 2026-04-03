package com.cloud.common.context;

import java.util.Collections;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContext {
  private Long userId;
  private String username;
  private String nickname;
  private Integer status;
  private String clientId;
  private String subject;
  @Builder.Default
  private Set<String> roles = Collections.emptySet();
  @Builder.Default
  private Set<String> permissions = Collections.emptySet();
  @Builder.Default
  private Set<String> scopes = Collections.emptySet();
}

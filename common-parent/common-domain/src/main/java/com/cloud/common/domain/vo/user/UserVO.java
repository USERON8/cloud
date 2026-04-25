package com.cloud.common.domain.vo.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private String username;

  private String phone;

  private String nickname;

  private String avatarUrl;

  private String email;

  private Integer status;

  private List<String> roles;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private LocalDateTime lastLoginAt;
}

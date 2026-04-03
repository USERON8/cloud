package com.cloud.common.domain.vo.user;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserVO {

  private Long id;

  private String username;

  private String phone;

  private String nickname;

  private String avatarUrl;

  private String email;

  private Integer status;

  private Integer enabled;

  private List<String> roles;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private LocalDateTime lastLoginAt;
}

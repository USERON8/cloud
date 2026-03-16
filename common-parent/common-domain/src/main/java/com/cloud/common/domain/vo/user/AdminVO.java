package com.cloud.common.domain.vo.user;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminVO {

  private Long id;

  private String username;

  private String realName;

  private String phone;

  private String role;

  private Integer status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

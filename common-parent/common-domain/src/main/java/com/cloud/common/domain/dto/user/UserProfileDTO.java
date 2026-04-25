package com.cloud.common.domain.dto.user;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class UserProfileDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private String username;

  private String phone;

  private String nickname;

  private String avatarUrl;

  private String email;

  private Integer status;
}

package com.cloud.common.domain.dto.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class AuthPrincipalDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  private String username;

  private String password;

  private String nickname;

  private String email;

  private String phone;

  private Integer status;

  private List<String> roles;

  private List<String> permissions;
}

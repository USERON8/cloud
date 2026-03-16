package com.cloud.common.domain.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public abstract class BaseAccountUpsertRequestDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Size(max = 50, message = "username length must be less than or equal to 50")
  private String username;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Size(max = 255, message = "password length must be less than or equal to 255")
  private String password;

  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "invalid phone format")
  private String phone;

  private Integer status;
}

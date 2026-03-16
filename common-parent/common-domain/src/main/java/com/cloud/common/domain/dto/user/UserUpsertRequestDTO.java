package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UserUpsertRequestDTO extends BaseAccountUpsertRequestDTO {

  private Long id;

  @Size(max = 50, message = "nickname length must be less than or equal to 50")
  private String nickname;

  @Size(max = 255, message = "avatar URL length must be less than or equal to 255")
  private String avatarUrl;

  @Email(message = "invalid email format")
  @Size(max = 100, message = "email length must be less than or equal to 100")
  private String email;

  @Min(value = 0, message = "status must be greater than or equal to 0")
  @Max(value = 1, message = "status must be less than or equal to 1")
  private List<String> roles;
}

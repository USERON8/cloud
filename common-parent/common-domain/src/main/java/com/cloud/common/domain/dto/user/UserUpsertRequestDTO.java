package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

  @Size(max = 10, message = "roles size must be less than or equal to 10")
  private List<
          @NotBlank(message = "role cannot be blank")
          @Size(max = 50, message = "role length must be less than or equal to 50") String>
      roles;

  private Integer enabled;
}

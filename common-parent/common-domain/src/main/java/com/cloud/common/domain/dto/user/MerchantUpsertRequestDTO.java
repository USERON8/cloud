package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MerchantUpsertRequestDTO extends BaseAccountUpsertRequestDTO {

  @Size(max = 100, message = "merchantName length must be less than or equal to 100")
  private String merchantName;

  @Email(message = "invalid email format")
  @Size(max = 100, message = "email length must be less than or equal to 100")
  private String email;
}

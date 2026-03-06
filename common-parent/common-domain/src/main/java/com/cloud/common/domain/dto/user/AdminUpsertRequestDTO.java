package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AdminUpsertRequestDTO extends BaseAccountUpsertRequestDTO {

    @Size(max = 50, message = "realName length must be less than or equal to 50")
    private String realName;

    @Size(max = 32, message = "role length must be less than or equal to 32")
    private String role;
}

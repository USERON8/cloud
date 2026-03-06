package com.cloud.common.domain.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AdminUpsertRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 50, message = "username length must be less than or equal to 50")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 255, message = "password length must be less than or equal to 255")
    private String password;

    @Size(max = 50, message = "realName length must be less than or equal to 50")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "invalid phone format")
    private String phone;

    @Size(max = 32, message = "role length must be less than or equal to 32")
    private String role;

    private Integer status;
}

package com.cloud.common.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "username cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "username must be 4-20 chars: letters, digits or underscore")
    private String username;

    @NotBlank(message = "password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
            message = "password must be 8-20 chars and include upper, lower and digit"
    )
    private String password;

    private String userType = "USER";
}

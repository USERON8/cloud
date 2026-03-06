package com.cloud.common.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "username cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "username must be 4-20 chars: letters, digits or underscore")
    private String username;

    @NotBlank(message = "password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
            message = "password must be 8-20 chars and include upper, lower and digit"
    )
    private String password;

    @NotBlank(message = "phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone format is invalid")
    private String phone;

    @NotBlank(message = "nickname cannot be blank")
    private String nickname;

    @Pattern(regexp = "^(USER|ADMIN|MERCHANT)$", message = "userType must be USER, ADMIN or MERCHANT")
    private String userType = "USER";
}

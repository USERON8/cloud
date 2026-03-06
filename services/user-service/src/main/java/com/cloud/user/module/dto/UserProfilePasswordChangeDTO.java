package com.cloud.user.module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserProfilePasswordChangeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "old password is required")
    @Size(max = 255, message = "old password length must be less than or equal to 255")
    private String oldPassword;

    @NotBlank(message = "new password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
            message = "new password must be 8-20 chars and include upper, lower and digit"
    )
    private String newPassword;
}

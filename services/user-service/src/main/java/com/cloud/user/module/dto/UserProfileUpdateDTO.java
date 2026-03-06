package com.cloud.user.module.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserProfileUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 50, message = "nickname length must be less than or equal to 50")
    private String nickname;

    @Size(max = 255, message = "avatar URL length must be less than or equal to 255")
    private String avatarUrl;

    @Email(message = "invalid email format")
    @Size(max = 100, message = "email length must be less than or equal to 100")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "invalid phone format")
    private String phone;
}

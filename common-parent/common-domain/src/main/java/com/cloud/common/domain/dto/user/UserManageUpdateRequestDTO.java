package com.cloud.common.domain.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserManageUpdateRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Size(max = 50, message = "username length must be less than or equal to 50")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 255, message = "password length must be less than or equal to 255")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "invalid phone format")
    private String phone;

    @Size(max = 50, message = "nickname length must be less than or equal to 50")
    private String nickname;

    @Size(max = 255, message = "avatar URL length must be less than or equal to 255")
    private String avatarUrl;

    @Email(message = "invalid email format")
    @Size(max = 100, message = "email length must be less than or equal to 100")
    private String email;

    @Min(value = 0, message = "status must be greater than or equal to 0")
    @Max(value = 1, message = "status must be less than or equal to 1")
    private Integer status;

    private List<String> roles;
}

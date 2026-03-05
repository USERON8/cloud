package com.cloud.common.domain.dto.user;

import com.cloud.common.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 50, message = "Username length must be less than or equal to 50")
    private String username;

    @Size(max = 255, message = "Password length must be less than or equal to 255")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone format")
    private String phone;

    @NotBlank(message = "Nickname cannot be blank")
    @Size(max = 50, message = "Nickname length must be less than or equal to 50")
    private String nickname;

    @Size(max = 255, message = "Avatar URL length must be less than or equal to 255")
    private String avatarUrl;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email length must be less than or equal to 100")
    private String email;

    private Long githubId;

    @Size(max = 100, message = "GitHub username length must be less than or equal to 100")
    private String githubUsername;

    @Size(max = 20, message = "OAuth provider length must be less than or equal to 20")
    private String oauthProvider;

    @Size(max = 100, message = "OAuth provider ID length must be less than or equal to 100")
    private String oauthProviderId;

    @Min(value = 0, message = "Status must be greater than or equal to 0")
    @Max(value = 1, message = "Status must be less than or equal to 1")
    private Integer status;

    private UserType userType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}

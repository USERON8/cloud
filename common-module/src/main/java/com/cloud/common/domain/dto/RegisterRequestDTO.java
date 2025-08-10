package com.cloud.common.domain.dto;

import lombok.Data;
import lombok.NonNull;

/**
 * 用户注册请求DTO
 */
@Data
public class RegisterRequestDTO {
    @NonNull
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String userType;
}
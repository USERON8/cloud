package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserProfileDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String phone;

    private String nickname;

    private String avatarUrl;

    private String email;

    private Integer status;
}

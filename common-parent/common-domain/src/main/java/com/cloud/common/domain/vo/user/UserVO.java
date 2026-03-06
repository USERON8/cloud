package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;




@Data
public class UserVO {
    


    private Long id;

    


    private String username;

    


    private String phone;

    


    private String nickname;

    


    private String avatarUrl;

    


    private String email;

    


    private Integer status;

    


    private String userType;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;

    


    private LocalDateTime lastLoginAt;
}

package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;






@Data
public class AdminVO {
    


    private Long id;

    


    private String username;

    


    private String realName;

    


    private String phone;

    


    private String role;

    


    private Integer status;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

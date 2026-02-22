package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;






@Data
public class AdminDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    


    private Long id;
    


    private String username;
    


    private String realName;
    


    private String phone;
    


    private String role;
    


    private Integer status;
    


    private LocalDateTime createdAt;
    


    private LocalDateTime updatedAt;
}

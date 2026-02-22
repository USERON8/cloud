package com.cloud.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;






@Data
public class MerchantVO {
    


    private Long id;

    


    private String username;

    


    private String merchantName;

    


    private String email;

    


    private String phone;

    


    private String userType;

    


    private Integer status;

    


    private Integer authStatus;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

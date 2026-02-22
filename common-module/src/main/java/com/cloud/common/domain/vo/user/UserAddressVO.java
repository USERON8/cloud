package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;







@Data
public class UserAddressVO {

    


    private Long id;

    


    private Long userId;

    


    private String consignee;

    


    private String phone;

    


    private String address;

    


    private Boolean isDefault;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

package com.cloud.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;






@Data
public class UserAddressVO {
    


    private Long id;

    


    private Long userId;

    


    private String consignee;

    


    private String phone;

    


    private String province;

    


    private String city;

    


    private String district;

    


    private String street;

    


    private String detailAddress;

    


    private Integer isDefault;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

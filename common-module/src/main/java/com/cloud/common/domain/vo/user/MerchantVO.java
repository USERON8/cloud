package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;







@Data
public class MerchantVO {

    


    private Long id;

    


    private String merchantName;

    


    private Integer status;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

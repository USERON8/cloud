package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.time.LocalDateTime;







@Data
public class MerchantShopDTO {

    


    private Long id;

    


    private Long shopId;

    


    private Long merchantId;

    


    private String shopName;

    


    private String shopCode;

    


    private Integer shopType;

    


    private String businessLicense;

    


    private String legalRepresentative;

    


    private String contactPhone;

    


    private Integer status;

    


    private LocalDateTime createTime;

    


    private LocalDateTime updateTime;
}

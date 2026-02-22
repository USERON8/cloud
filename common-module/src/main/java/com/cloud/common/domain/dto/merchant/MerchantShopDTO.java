package com.cloud.common.domain.dto.merchant;

import lombok.Data;

import java.time.LocalDateTime;







@Data
public class MerchantShopDTO {

    


    private Long id;

    


    private Long merchantId;

    


    private String shopName;

    


    private String shopCode;

    


    private String description;

    


    private Integer status;

    


    private Integer shopType;

    


    private String contactPhone;

    


    private String address;

    


    private String businessLicense;

    


    private String legalRepresentative;

    


    private LocalDateTime createTime;

    


    private LocalDateTime updateTime;
}

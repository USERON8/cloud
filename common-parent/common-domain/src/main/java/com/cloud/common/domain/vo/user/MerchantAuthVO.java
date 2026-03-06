package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;






@Data
public class MerchantAuthVO {
    


    private Long id;

    


    private Long merchantId;

    


    private String businessLicenseNumber;

    


    private String businessLicenseUrl;

    


    private String idCardFrontUrl;

    


    private String idCardBackUrl;

    


    private String contactPhone;

    


    private String contactAddress;

    


    private Integer authStatus;

    


    private String authRemark;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}

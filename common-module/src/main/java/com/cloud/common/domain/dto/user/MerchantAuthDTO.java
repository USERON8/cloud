package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;




@Data
public class MerchantAuthDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    


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

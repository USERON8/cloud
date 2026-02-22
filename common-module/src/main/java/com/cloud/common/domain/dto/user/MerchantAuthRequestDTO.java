package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;




@Data
public class MerchantAuthRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    


    private String businessLicenseNumber;
    


    private String businessLicenseUrl;
    


    private String idCardFrontUrl;
    


    private String idCardBackUrl;
    


    private String contactPhone;
    


    private String contactAddress;
}

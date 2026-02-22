package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;







@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantAuthPageDTO extends PageQuery {

    


    private Long merchantId;

    


    private Integer authStatus;

    


    private String merchantName;

    


    private String companyName;
}

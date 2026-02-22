package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;







@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPageDTO extends PageQuery {

    


    private String merchantName;

    


    private Integer status;

    


    private Integer authStatus;

    


    private String merchantType;
}

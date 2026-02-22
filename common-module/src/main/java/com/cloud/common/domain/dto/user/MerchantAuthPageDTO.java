package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;




@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantAuthPageDTO extends PageQuery {
    


    private Integer authStatus;

    


    private String merchantName;
}

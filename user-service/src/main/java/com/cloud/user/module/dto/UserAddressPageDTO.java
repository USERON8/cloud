package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;







@Data
@EqualsAndHashCode(callSuper = true)
public class UserAddressPageDTO extends PageQuery {

    


    private Long userId;

    


    private String consignee;

    


    private String province;

    


    private String city;

    


    private Integer isDefault;
}

package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;




@Data
@EqualsAndHashCode(callSuper = true)
public class UserAddressPageDTO extends PageQuery {
    


    private Long userId;

    


    private String consignee;
}

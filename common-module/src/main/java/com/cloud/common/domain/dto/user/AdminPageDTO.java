package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;




@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPageDTO extends PageQuery {
    


    private String username;

    


    private String phone;

    


    private Integer status;

    


    private String role;
}

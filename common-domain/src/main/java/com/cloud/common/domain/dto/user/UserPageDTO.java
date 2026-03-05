package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;




@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageDTO extends PageQuery {
    


    private String username;

    private String email;

    


    private String phone;

    


    private String nickname;

    


    private Integer status;

    


    private String userType;
}

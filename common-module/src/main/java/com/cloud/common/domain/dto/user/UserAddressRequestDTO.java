package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;




@EqualsAndHashCode(callSuper = true)
@Data
public class UserAddressRequestDTO extends BaseEntity<UserAddressRequestDTO> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    


    private String consignee;
    


    private String phone;
    


    private String province;
    


    private String city;
    


    private String district;
    


    private String street;
    


    private String detailAddress;
    


    private Integer isDefault;
}

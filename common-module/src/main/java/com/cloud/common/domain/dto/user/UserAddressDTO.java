package com.cloud.common.domain.dto.user;

import lombok.Data;

@Data
public class UserAddressDTO {
    private String id;
    private String userId;
    private String consignee;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String street;
    private String detailAddress;
    private Integer isDefault;
}

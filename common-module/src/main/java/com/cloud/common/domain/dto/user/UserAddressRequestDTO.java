package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户地址请求DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserAddressRequestDTO extends BaseEntity<UserAddressRequestDTO> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 收货人姓名
     */
    private String consignee;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 区县
     */
    private String district;
    /**
     * 街道
     */
    private String street;
    /**
     * 详细地址
     */
    private String detailAddress;
    /**
     * 是否默认地址：0-否，1-是
     */
    private Integer isDefault;
}
package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户地址信息DTO
 *
 * @author what's up
 */
@Data
public class UserAddressDTO {
    /**
     * 地址ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户地址表
 *
 * @author what's up
 * @TableName user_address
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "user_address")
@Data
public class UserAddress extends BaseEntity<UserAddress> {
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 收货人姓名
     */
    @TableField(value = "consignee")
    private String consignee;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 省份
     */
    @TableField(value = "province")
    private String province;

    /**
     * 城市
     */
    @TableField(value = "city")
    private String city;

    /**
     * 区县
     */
    @TableField(value = "district")
    private String district;

    /**
     * 街道
     */
    @TableField(value = "street")
    private String street;

    /**
     * 详细地址
     */
    @TableField(value = "detail_address")
    private String detailAddress;

    /**
     * 是否默认地址：0-否，1-是
     */
    @TableField(value = "is_default")
    private Integer isDefault;
}
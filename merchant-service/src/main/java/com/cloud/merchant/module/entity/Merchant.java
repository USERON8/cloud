package com.cloud.merchant.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家信息表
 *
 * @TableName merchant
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant")
@Data
public class Merchant extends BaseEntity<Merchant> {
    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 加密密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 商家名称
     */
    @TableField(value = "merchant_name")
    private String merchantName;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 用户类型
     */
    @TableField(value = "user_type")
    private String userType;
}
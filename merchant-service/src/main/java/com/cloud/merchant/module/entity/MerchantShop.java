package com.cloud.merchant.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商家店铺信息表
 *
 * @TableName merchant_shop
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_shop")
@Data
public class MerchantShop extends BaseEntity<MerchantShop> {
    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 店铺名称
     */
    @TableField(value = "shop_name")
    private String shopName;

    /**
     * 店铺头像URL
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 店铺头像文件名
     */
    @TableField(value = "avatar_file_name")
    private String avatarFileName;

    /**
     * 店铺描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 客服电话
     */
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 详细地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 经度
     */
    @TableField(value = "longitude")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @TableField(value = "latitude")
    private BigDecimal latitude;

    /**
     * 状态：0-关闭，1-营业，2-暂停营业
     */
    @TableField(value = "status")
    private Integer status;
}
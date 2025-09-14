package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家店铺表
 *
 * @TableName merchant_shop
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_shop")
@Data
public class Shop extends BaseEntity<Shop> {
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
     * 状态：0-关闭，1-营业
     */
    @TableField(value = "status")
    private Integer status;
    
    // 为了兼容性提供getName方法
    public String getName() {
        return this.shopName;
    }
    
    public void setName(String name) {
        this.shopName = name;
    }
}

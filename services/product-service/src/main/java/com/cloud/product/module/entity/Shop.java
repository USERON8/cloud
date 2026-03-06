package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;






@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_shop")
@Data
public class Shop extends BaseEntity<Shop> {
    


    @TableField(value = "merchant_id")
    private Long merchantId;

    


    @TableField(value = "shop_name")
    private String shopName;

    


    @TableField(value = "avatar_url")
    private String avatarUrl;

    


    @TableField(value = "description")
    private String description;

    


    @TableField(value = "contact_phone")
    private String contactPhone;

    


    @TableField(value = "address")
    private String address;

    


    @TableField(value = "status")
    private Integer status;

    
    public String getName() {
        return this.shopName;
    }

    public void setName(String name) {
        this.shopName = name;
    }
}

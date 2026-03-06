package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant")
@Data
public class Merchant extends BaseEntity<Merchant> {
    


    @TableField(value = "username")
    private String username;

    


    @TableField(value = "password")
    private String password;

    


    @TableField(value = "merchant_name")
    private String merchantName;

    


    @TableField(value = "phone")
    private String phone;

    


    @TableField(value = "status")
    private Integer status;
}

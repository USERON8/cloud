package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "admin")
@Data
public class Admin extends BaseEntity<Admin> {
    


    @TableField(value = "username")
    private String username;

    


    @TableField(value = "password")
    private String password;

    


    @TableField(value = "real_name")
    private String realName;

    


    @TableField(value = "phone")
    private String phone;

    


    @TableField(value = "role")
    private String role;

    


    @TableField(value = "status")
    private Integer status;
}

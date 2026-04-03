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

  @TableField(value = "merchant_name")
  private String merchantName;

  @TableField(value = "phone")
  private String phone;

  @TableField(value = "email")
  private String email;

  @TableField(value = "status")
  private Integer status;

  @TableField(value = "audit_status")
  private Integer auditStatus;
}

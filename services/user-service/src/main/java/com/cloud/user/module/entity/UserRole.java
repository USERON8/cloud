package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "user_roles")
@Data
public class UserRole extends BaseEntity<UserRole> {

  @TableField("user_id")
  private Long userId;

  @TableField("role_id")
  private Long roleId;
}

package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "user_address")
@Data
public class UserAddress extends BaseEntity<UserAddress> {

  @TableField(value = "user_id")
  private Long userId;

  @TableField(value = "address_tag")
  private String addressTag;

  @TableField(value = "receiver_name")
  private String receiverName;

  @TableField(value = "receiver_phone")
  private String receiverPhone;

  @TableField(value = "country")
  private String country;

  @TableField(value = "province")
  private String province;

  @TableField(value = "city")
  private String city;

  @TableField(value = "district")
  private String district;

  @TableField(value = "street")
  private String street;

  @TableField(value = "detail_address")
  private String detailAddress;

  @TableField(value = "postal_code")
  private String postalCode;

  @TableField(value = "longitude")
  private BigDecimal longitude;

  @TableField(value = "latitude")
  private BigDecimal latitude;

  @TableField(value = "is_default")
  private Integer isDefault;
}

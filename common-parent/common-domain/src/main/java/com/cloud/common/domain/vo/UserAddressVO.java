package com.cloud.common.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserAddressVO {

  private Long id;

  private Long userId;

  private String addressTag;

  private String receiverName;

  private String receiverPhone;

  private String country;

  private String province;

  private String city;

  private String district;

  private String street;

  private String detailAddress;

  private String postalCode;

  private BigDecimal longitude;

  private BigDecimal latitude;

  private Integer isDefault;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

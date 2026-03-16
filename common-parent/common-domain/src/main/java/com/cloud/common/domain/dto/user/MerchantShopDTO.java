package com.cloud.common.domain.dto.user;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MerchantShopDTO {

  private Long id;

  private Long shopId;

  private Long merchantId;

  private String shopName;

  private String shopCode;

  private Integer shopType;

  private String businessLicense;

  private String legalRepresentative;

  private String contactPhone;

  private Integer status;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;
}

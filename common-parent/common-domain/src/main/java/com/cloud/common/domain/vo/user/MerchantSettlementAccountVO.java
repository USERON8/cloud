package com.cloud.common.domain.vo.user;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MerchantSettlementAccountVO {

  private Long id;

  private Long merchantId;

  private String accountName;

  private String accountNumber;

  private Integer accountType;

  private String bankName;

  private Integer isDefault;

  private Integer status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

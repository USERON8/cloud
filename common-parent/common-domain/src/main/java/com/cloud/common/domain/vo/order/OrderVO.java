package com.cloud.common.domain.vo.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderVO {
  private Long id;

  private String orderNo;

  private Long userId;

  private Long shopId;

  private BigDecimal totalAmount;

  private BigDecimal payAmount;

  private Integer status;

  private Long addressId;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

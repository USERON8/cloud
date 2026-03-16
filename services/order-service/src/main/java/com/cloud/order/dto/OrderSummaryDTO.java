package com.cloud.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderSummaryDTO {
  private Long id;
  private String orderNo;
  private Long userId;
  private BigDecimal totalAmount;
  private BigDecimal payAmount;
  private Integer status;
  private LocalDateTime createdAt;
}

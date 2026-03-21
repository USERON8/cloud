package com.cloud.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderSummaryDTO {
  private Long id;
  private String orderNo;
  private Long userId;
  private Long subOrderId;
  private String subOrderNo;
  private Long merchantId;
  private Long afterSaleId;
  private String afterSaleNo;
  private BigDecimal totalAmount;
  private BigDecimal payAmount;
  private Integer status;
  private String afterSaleStatus;
  private LocalDateTime createdAt;
}

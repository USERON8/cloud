package com.cloud.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AfterSaleDTO {

  private Long id;

  private String afterSaleNo;

  private Long mainOrderId;

  private Long subOrderId;

  private Long userId;

  private Long merchantId;

  private String afterSaleType;

  private String status;

  private String reason;

  private String description;

  private BigDecimal applyAmount;

  private BigDecimal approvedAmount;

  private String returnLogisticsCompany;

  private String returnLogisticsNo;

  private String refundChannel;

  private LocalDateTime refundedAt;

  private LocalDateTime closedAt;

  private String closeReason;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private Integer deleted;

  private Integer version;
}

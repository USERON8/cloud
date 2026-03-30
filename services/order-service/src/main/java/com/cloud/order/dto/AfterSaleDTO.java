package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "After-sale request and response payload")
public class AfterSaleDTO {

  @Schema(description = "After-sale id")
  private Long id;

  @Schema(description = "After-sale number")
  private String afterSaleNo;

  @NotNull(message = "mainOrderId is required")
  @Schema(description = "Main order id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long mainOrderId;

  @Schema(description = "Sub order id")
  private Long subOrderId;

  @Schema(description = "User id")
  private Long userId;

  @Schema(description = "Merchant id")
  private Long merchantId;

  @NotBlank(message = "afterSaleType is required")
  @Schema(description = "After-sale type", requiredMode = Schema.RequiredMode.REQUIRED)
  private String afterSaleType;

  @Schema(description = "After-sale status")
  private String status;

  @NotBlank(message = "reason is required")
  @Schema(description = "After-sale reason", requiredMode = Schema.RequiredMode.REQUIRED)
  private String reason;

  @Schema(description = "Detailed description")
  private String description;

  @NotNull(message = "applyAmount is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "applyAmount must be greater than 0")
  @Schema(description = "Requested refund amount", requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal applyAmount;

  @Schema(description = "Approved refund amount")
  private BigDecimal approvedAmount;

  @Schema(description = "Return logistics company")
  private String returnLogisticsCompany;

  @Schema(description = "Return logistics number")
  private String returnLogisticsNo;

  @Schema(description = "Refund channel")
  private String refundChannel;

  @Schema(description = "Refund completion time")
  private LocalDateTime refundedAt;

  @Schema(description = "After-sale close time")
  private LocalDateTime closedAt;

  @Schema(description = "Close reason")
  private String closeReason;

  @Schema(description = "Creation time")
  private LocalDateTime createdAt;

  @Schema(description = "Update time")
  private LocalDateTime updatedAt;

  @Schema(description = "Soft delete flag")
  private Integer deleted;

  @Schema(description = "Optimistic lock version")
  private Integer version;
}

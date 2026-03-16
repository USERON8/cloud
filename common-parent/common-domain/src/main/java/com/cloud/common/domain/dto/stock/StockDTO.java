package com.cloud.common.domain.dto.stock;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockDTO {

  private Long id;

  private Long productId;

  private String productName;

  private Integer stockQuantity;

  private Integer frozenQuantity;

  private Integer availableQuantity;

  private Integer stockStatus;

  private Integer lowStockThreshold;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

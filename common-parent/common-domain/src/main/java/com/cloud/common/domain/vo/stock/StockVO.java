package com.cloud.common.domain.vo.stock;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockVO {

  private Long id;

  private String productName;

  private Long productId;

  private Integer stockQuantity;

  private Integer frozenQuantity;

  private Integer availableQuantity;

  private Integer stockStatus;

  private Integer lowStockThreshold;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

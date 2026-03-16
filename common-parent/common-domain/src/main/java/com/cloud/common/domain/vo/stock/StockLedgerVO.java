package com.cloud.common.domain.vo.stock;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockLedgerVO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;
  private Long skuId;
  private Integer onHandQty;
  private Integer reservedQty;
  private Integer salableQty;
  private Integer alertThreshold;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

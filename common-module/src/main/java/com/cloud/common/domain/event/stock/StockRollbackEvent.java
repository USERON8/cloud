package com.cloud.common.domain.event.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存回滚事件
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRollbackEvent {
    
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String rollbackReason;
    private String rollbackType;
    private List<StockRollbackItem> rollbackItems;
    private LocalDateTime eventTime;
    private String traceId;
    private String operator;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockRollbackItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private String specification;
        private Long warehouseId;
    }
}

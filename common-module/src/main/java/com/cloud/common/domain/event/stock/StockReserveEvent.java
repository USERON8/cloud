package com.cloud.common.domain.event.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预留事件
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveEvent {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 预留原因
     */
    private String reserveReason;

    /**
     * 预留类型
     */
    private String reserveType;

    /**
     * 预留项列表
     */
    private List<StockReserveItem> reserveItems;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 库存预留项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReserveItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private String specification;
        private Long warehouseId;
    }
}

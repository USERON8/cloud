package com.cloud.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存类型变更事件对象
 * 用于在服务间传递库存类型变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTypeChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 变更类型：1-创建库存类型，2-更新库存类型，3-删除库存类型
     */
    private Integer changeType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;
}
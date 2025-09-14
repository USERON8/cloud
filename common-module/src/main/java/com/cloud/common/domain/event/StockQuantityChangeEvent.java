package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存变更事件对象
 * 用于在服务间传递库存变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuantityChangeEvent implements Serializable {
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
     * 变更前数量
     */
    private Integer beforeCount;

    /**
     * 变更数量
     */
    private Integer changeCount;

    /**
     * 变更后数量
     */
    private Integer afterCount;

    /**
     * 可用库存数量
     */
    private Integer availableCount;

    /**
     * 冻结库存数量
     */
    private Integer frozenCount;

    /**
     * 库存状态：1-正常，2-缺货，3-下架
     */
    private Integer stockStatus;

    /**
     * 变更类型：1-增加库存，2-扣减库存，3-冻结库存，4-解冻库存
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
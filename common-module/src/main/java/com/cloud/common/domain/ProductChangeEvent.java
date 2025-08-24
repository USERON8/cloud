package com.cloud.common.domain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品变更事件对象
 * 用于在服务间传递商品变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
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
     * 变更类型：1-创建商品，2-更新商品，3-删除商品
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
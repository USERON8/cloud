package com.cloud.common.domain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单变更事件对象
 * 用于在服务间传递订单变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 变更前状态
     */
    private Integer beforeStatus;
    
    /**
     * 变更后状态
     */
    private Integer afterStatus;
    
    /**
     * 变更类型：1-创建订单，2-更新订单，3-删除订单
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
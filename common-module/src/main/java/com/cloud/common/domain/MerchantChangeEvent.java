package com.cloud.common.domain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家变更事件对象
 * 用于在服务间传递商家变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 商家ID
     */
    private Long merchantId;
    
    /**
     * 商家名称
     */
    private String merchantName;
    
    /**
     * 变更前状态
     */
    private Integer beforeStatus;
    
    /**
     * 变更后状态
     */
    private Integer afterStatus;
    
    /**
     * 变更类型：1-创建商家，2-更新商家，3-删除商家
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
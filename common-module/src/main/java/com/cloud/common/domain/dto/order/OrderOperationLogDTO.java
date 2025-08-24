package com.cloud.common.domain.dto.order;

import lombok.Data;

/**
 * 订单操作日志DTO
 */
@Data
public class OrderOperationLogDTO {
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 原状态
     */
    private Integer fromStatus;

    /**
     * 目标状态
     */
    private Integer toStatus;

    /**
     * 操作人（系统/用户ID）
     */
    private String operator;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;
}
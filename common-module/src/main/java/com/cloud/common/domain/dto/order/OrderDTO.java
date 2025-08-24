package com.cloud.common.domain.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单DTO
 */
@Data
public class OrderDTO {
    /**
     * 订单ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消
     */
    private Integer status;

    /**
     * 地址快照id
     */
    private String address_id;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标识
     */
    private Integer deleted;
}
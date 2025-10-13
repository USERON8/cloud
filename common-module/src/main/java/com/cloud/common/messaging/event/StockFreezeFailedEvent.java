package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 库存冻结失败事件
 * 当库存冻结失败时发送此事件，通知订单服务取消订单
 *
 * @author what's up
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFreezeFailedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 事件ID
     */
    private String eventId;
}

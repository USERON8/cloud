package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单创建事件
 * 当订单创建时发送此事件，通知库存服务冻结库存、支付服务创建支付
 *
 * @author what's up
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

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
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 商品数量映射 Map<商品ID, 购买数量>
     */
    private Map<Long, Integer> productQuantityMap;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 事件ID（用于幂等性）
     */
    private String eventId;
}

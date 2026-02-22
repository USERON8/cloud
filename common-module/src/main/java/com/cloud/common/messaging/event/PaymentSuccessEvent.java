package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付成功事件
 * 当支付完成时发送此事件，通知订单服务完成订单、库存服务扣减库存
 *
 * @author what's up
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付ID
     */
    private Long paymentId;

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
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付流水号
     */
    private String transactionNo;

    /**
     * 商品数量映射 Map<商品ID, 购买数量>
     */
    private Map<Long, Integer> productQuantityMap;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 事件ID（用于幂等性）
     */
    private String eventId;
}

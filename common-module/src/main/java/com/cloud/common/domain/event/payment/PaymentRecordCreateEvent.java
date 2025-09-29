package com.cloud.common.domain.event.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录创建事件
 * 
 * 当订单创建完成需要生成支付记录时发送此事件，
 * 通知支付服务创建支付记录，准备支付流程
 * 
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordCreateEvent implements Serializable {

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
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 原始订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 支付方式
     * ALIPAY - 支付宝
     * WECHAT - 微信支付
     * BANK_CARD - 银行卡
     */
    private String paymentMethod;

    /**
     * 支付超时时间（分钟）
     */
    private Integer timeoutMinutes;

    /**
     * 订单商品信息（JSON格式）
     */
    private String orderItems;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 分布式追踪ID
     */
    private String traceId;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;
}

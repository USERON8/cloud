package com.cloud.common.domain.event.payment;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 支付操作日志事件
 * <p>
 * 专门记录支付的关键操作日志，包括：
 * - 支付成功
 * - 支付退款成功
 * 其他支付状态变更暂不记录到日志系统
 *
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentOperationLogEvent extends BaseBusinessLogEvent {

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
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 退款金额（退款操作时使用）
     */
    private BigDecimal refundAmount;

    /**
     * 支付方式
     * ALIPAY - 支付宝
     * WECHAT - 微信支付
     * BANK_CARD - 银行卡
     * BALANCE - 余额支付
     */
    private String paymentMethod;

    /**
     * 第三方支付交易号
     */
    private String thirdPartyTransactionId;

    /**
     * 支付状态变更
     * PENDING -> SUCCESS（待支付->支付成功）
     * SUCCESS -> REFUNDED（支付成功->已退款）
     */
    private String statusChange;

    /**
     * 退款原因（退款操作时使用）
     */
    private String refundReason;

    /**
     * 退款类型
     * FULL - 全额退款
     * PARTIAL - 部分退款
     */
    private String refundType;

    /**
     * 退款ID（退款操作时使用）
     */
    private Long refundId;

    /**
     * 支付完成时间或退款完成时间
     */
    private String completionTime;

    /**
     * 支付渠道返回码
     */
    private String channelReturnCode;

    /**
     * 支付渠道返回消息
     */
    private String channelReturnMessage;

    @Override
    public String getLogType() {
        return "PAYMENT_OPERATION";
    }
}

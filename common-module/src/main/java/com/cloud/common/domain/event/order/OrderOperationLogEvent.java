package com.cloud.common.domain.event.order;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 订单操作日志事件
 * <p>
 * 专门记录订单的关键操作日志，包括：
 * - 订单完成
 * - 订单退款
 * 其他订单状态变更暂不记录到日志系统
 *
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderOperationLogEvent extends BaseBusinessLogEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 退款金额（退款操作时使用）
     */
    private BigDecimal refundAmount;

    /**
     * 订单状态变更
     * PAID -> COMPLETED（已支付->已完成）
     * COMPLETED -> REFUNDED（已完成->已退款）
     * CANCELLED -> REFUNDED（已取消->已退款）
     */
    private String statusChange;

    /**
     * 退款原因（退款操作时使用）
     */
    private String refundReason;

    /**
     * 退款类型
     * CANCEL - 订单取消退款
     * RETURN - 订单退货退款
     * PARTIAL - 部分退款
     */
    private String refundType;

    /**
     * 商品总数量
     */
    private Integer totalQuantity;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 完成时间或退款时间
     */
    private String completionTime;

    @Override
    public String getLogType() {
        return "ORDER_OPERATION";
    }
}

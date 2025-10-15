package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款/退货单实体
 * 对应数据库表: refunds
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@TableName(value = "refunds")
@Data
public class Refund {

    /**
     * 退货单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 退货单号
     */
    @TableField(value = "refund_no")
    private String refundNo;

    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 订单号
     */
    @TableField(value = "order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 退货类型：1-仅退款，2-退货退款
     */
    @TableField(value = "refund_type")
    private Integer refundType;

    /**
     * 退货原因
     */
    @TableField(value = "refund_reason")
    private String refundReason;

    /**
     * 详细说明
     */
    @TableField(value = "refund_description")
    private String refundDescription;

    /**
     * 退款金额
     */
    @TableField(value = "refund_amount")
    private BigDecimal refundAmount;

    /**
     * 退货数量
     */
    @TableField(value = "refund_quantity")
    private Integer refundQuantity;

    /**
     * 退货状态：0-待审核，1-审核通过，2-审核拒绝，3-退货中，4-已收货，5-退款中，6-已完成，7-已取消，8-已关闭
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 审核时间
     */
    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核备注
     */
    @TableField(value = "audit_remark")
    private String auditRemark;

    /**
     * 物流公司
     */
    @TableField(value = "logistics_company")
    private String logisticsCompany;

    /**
     * 物流单号
     */
    @TableField(value = "logistics_no")
    private String logisticsNo;

    /**
     * 退款时间
     */
    @TableField(value = "refund_time")
    private LocalDateTime refundTime;

    /**
     * 退款渠道
     */
    @TableField(value = "refund_channel")
    private String refundChannel;

    /**
     * 退款交易号
     */
    @TableField(value = "refund_transaction_no")
    private String refundTransactionNo;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // ===================== 业务方法 =====================

    /**
     * 生成退款单号
     *
     * @return 退款单号
     */
    public static String generateRefundNo() {
        return "REF" + System.currentTimeMillis() + String.format("%03d", (int) (Math.random() * 1000));
    }

    /**
     * 是否为仅退款类型
     */
    public boolean isRefundOnly() {
        return refundType != null && refundType == 1;
    }

    /**
     * 是否为退货退款类型
     */
    public boolean isReturnAndRefund() {
        return refundType != null && refundType == 2;
    }

    /**
     * 是否待审核
     */
    public boolean isPendingAudit() {
        return status != null && status == 0;
    }

    /**
     * 是否审核通过
     */
    public boolean isAuditPassed() {
        return status != null && status == 1;
    }

    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return status != null && status == 6;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return status != null && (status == 0 || status == 1);
    }
}

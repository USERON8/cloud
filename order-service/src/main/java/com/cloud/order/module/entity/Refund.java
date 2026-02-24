package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;








@TableName(value = "refunds")
@Data
public class Refund {

    


    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    


    @TableField(value = "refund_no")
    private String refundNo;

    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "order_no")
    private String orderNo;

    


    @TableField(value = "user_id")
    private Long userId;

    


    @TableField(value = "merchant_id")
    private Long merchantId;

    


    @TableField(value = "refund_type")
    private Integer refundType;

    


    @TableField(value = "refund_reason")
    private String refundReason;

    


    @TableField(value = "refund_description")
    private String refundDescription;

    


    @TableField(value = "refund_amount")
    private BigDecimal refundAmount;

    


    @TableField(value = "refund_quantity")
    private Integer refundQuantity;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    


    @TableField(value = "audit_remark")
    private String auditRemark;

    


    @TableField(value = "logistics_company")
    private String logisticsCompany;

    


    @TableField(value = "logistics_no")
    private String logisticsNo;

    


    @TableField(value = "refund_time")
    private LocalDateTime refundTime;

    


    @TableField(value = "refund_channel")
    private String refundChannel;

    


    @TableField(value = "refund_transaction_no")
    private String refundTransactionNo;

    


    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    


    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    


    @TableField(value = "is_deleted")
    private Integer isDeleted;

    

    




    public static String generateRefundNo() {
        return "REF" + System.currentTimeMillis() + String.format("%03d", (int) (Math.random() * 1000));
    }

    


    public boolean isRefundOnly() {
        return refundType != null && refundType == 1;
    }

    


    public boolean isReturnAndRefund() {
        return refundType != null && refundType == 2;
    }

    


    public boolean isPendingAudit() {
        return status != null && status == 0;
    }

    


    public boolean isAuditPassed() {
        return status != null && status == 1;
    }

    


    public boolean isCompleted() {
        return status != null && status == 6;
    }

    


    public boolean canCancel() {
        return status != null && (status == 0 || status == 1);
    }
}

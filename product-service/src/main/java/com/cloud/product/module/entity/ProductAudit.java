package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_audit")
@Data
public class ProductAudit extends BaseEntity<ProductAudit> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "product_name")
    private String productName;

    


    @TableField(value = "merchant_id")
    private Long merchantId;

    


    @TableField(value = "merchant_name")
    private String merchantName;

    


    @TableField(value = "audit_status")
    private String auditStatus;

    


    @TableField(value = "audit_type")
    private String auditType;

    


    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    


    @TableField(value = "auditor_id")
    private Long auditorId;

    


    @TableField(value = "auditor_name")
    private String auditorName;

    


    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    


    @TableField(value = "audit_comment")
    private String auditComment;

    


    @TableField(value = "reject_reason")
    private String rejectReason;

    


    @TableField(value = "product_snapshot")
    private String productSnapshot;

    


    @TableField(value = "priority")
    private Integer priority;
}

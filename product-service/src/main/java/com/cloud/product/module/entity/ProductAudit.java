package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品审核记录表
 *
 * @author what's up
 * @TableName product_audit
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_audit")
@Data
public class ProductAudit extends BaseEntity<ProductAudit> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品名称 (冗余字段,方便查询)
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 商家名称
     */
    @TableField(value = "merchant_name")
    private String merchantName;

    /**
     * 审核状态: PENDING-待审核, APPROVED-审核通过, REJECTED-审核拒绝
     */
    @TableField(value = "audit_status")
    private String auditStatus;

    /**
     * 审核类型: CREATE-新建审核, UPDATE-更新审核, PRICE-价格变更审核
     */
    @TableField(value = "audit_type")
    private String auditType;

    /**
     * 提交时间
     */
    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    /**
     * 审核人ID
     */
    @TableField(value = "auditor_id")
    private Long auditorId;

    /**
     * 审核人姓名
     */
    @TableField(value = "auditor_name")
    private String auditorName;

    /**
     * 审核时间
     */
    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    @TableField(value = "audit_comment")
    private String auditComment;

    /**
     * 拒绝原因
     */
    @TableField(value = "reject_reason")
    private String rejectReason;

    /**
     * 商品快照 (JSON格式,记录提交审核时的商品数据)
     */
    @TableField(value = "product_snapshot")
    private String productSnapshot;

    /**
     * 优先级: 1-低, 2-中, 3-高, 4-紧急
     */
    @TableField(value = "priority")
    private Integer priority;
}

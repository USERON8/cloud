package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 品牌授权表
 * 记录商家对品牌的使用授权
 *
 * @author what's up
 * @TableName brand_authorization
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "brand_authorization")
@Data
public class BrandAuthorization extends BaseEntity<BrandAuthorization> {
    /**
     * 品牌ID
     */
    @TableField(value = "brand_id")
    private Long brandId;

    /**
     * 品牌名称 (冗余字段)
     */
    @TableField(value = "brand_name")
    private String brandName;

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
     * 授权类型: OFFICIAL-官方旗舰店, AUTHORIZED-授权经销商, DISTRIBUTOR-分销商
     */
    @TableField(value = "auth_type")
    private String authType;

    /**
     * 授权状态: PENDING-待审核, APPROVED-已授权, REJECTED-已拒绝, EXPIRED-已过期, REVOKED-已撤销
     */
    @TableField(value = "auth_status")
    private String authStatus;

    /**
     * 授权证书URL
     */
    @TableField(value = "certificate_url")
    private String certificateUrl;

    /**
     * 授权开始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 授权结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

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
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
}

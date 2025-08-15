package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家认证信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_auth")
public class MerchantAuth extends BaseEntity {
    /**
     * 认证ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 店铺名称
     */
    @TableField("shop_name")
    private String shopName;

    /**
     * 营业执照号码
     */
    @TableField("business_license_number")
    private String businessLicenseNumber;

    /**
     * 营业执照图片URL
     */
    @TableField("business_license_url")
    private String businessLicenseUrl;

    /**
     * 身份证正面URL
     */
    @TableField("id_card_front_url")
    private String idCardFrontUrl;

    /**
     * 身份证反面URL
     */
    @TableField("id_card_back_url")
    private String idCardBackUrl;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 联系地址
     */
    @TableField("contact_address")
    private String contactAddress;

    /**
     * 审核状态：0-待审核，1-审核通过，2-审核拒绝
     */
    @TableField("status")
    private Integer status;

    /**
     * 审核备注
     */
    @TableField("audit_remark")
    private String auditRemark;
}
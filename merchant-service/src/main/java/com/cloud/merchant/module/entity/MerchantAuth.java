package com.cloud.merchant.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家认证信息表
 *
 * @TableName merchant_auth
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_auth")
@Data
public class MerchantAuth extends BaseEntity<MerchantAuth> {
    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 营业执照号码
     */
    @TableField(value = "business_license_number")
    private String businessLicenseNumber;

    /**
     * 营业执照图片URL
     */
    @TableField(value = "business_license_url")
    private String businessLicenseUrl;

    /**
     * 身份证正面URL
     */
    @TableField(value = "id_card_front_url")
    private String idCardFrontUrl;

    /**
     * 身份证反面URL
     */
    @TableField(value = "id_card_back_url")
    private String idCardBackUrl;

    /**
     * 联系电话
     */
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 联系地址
     */
    @TableField(value = "contact_address")
    private String contactAddress;

    /**
     * 认证状态：0-待审核，1-审核通过，2-审核拒绝
     */
    @TableField(value = "auth_status")
    private Integer authStatus;

    /**
     * 审核备注
     */
    @TableField(value = "auth_remark")
    private String authRemark;
}
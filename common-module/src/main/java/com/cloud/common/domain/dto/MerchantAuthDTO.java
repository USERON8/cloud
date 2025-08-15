package com.cloud.common.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家认证信息DTO
 */
@Data
public class MerchantAuthDTO {
    /**
     * 认证ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 营业执照号码
     */
    private String businessLicenseNumber;

    /**
     * 营业执照图片URL
     */
    private String businessLicenseUrl;

    /**
     * 身份证正面URL
     */
    private String idCardFrontUrl;

    /**
     * 身份证反面URL
     */
    private String idCardBackUrl;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系地址
     */
    private String contactAddress;

    /**
     * 审核状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer status;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家认证信息VO
 *
 * @author what's up
 */
@Data
public class MerchantAuthVO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 商家ID
     */
    private Long merchantId;

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
     * 认证状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer authStatus;

    /**
     * 审核备注
     */
    private String authRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
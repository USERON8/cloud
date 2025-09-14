package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商家认证申请DTO
 */
@Data
public class MerchantAuthRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
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
}
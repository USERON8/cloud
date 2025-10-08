package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家店铺DTO
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class MerchantShopDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺编码
     */
    private String shopCode;

    /**
     * 店铺类型
     */
    private Integer shopType;

    /**
     * 营业执照
     */
    private String businessLicense;

    /**
     * 法定代表人
     */
    private String legalRepresentative;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 店铺状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

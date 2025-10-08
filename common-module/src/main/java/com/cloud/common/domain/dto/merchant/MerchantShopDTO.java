package com.cloud.common.domain.dto.merchant;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家店铺DTO
 *
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
public class MerchantShopDTO {

    /**
     * 店铺ID
     */
    private Long id;

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
     * 店铺描述
     */
    private String description;

    /**
     * 店铺状态：0-关闭，1-营业，2-审核中，3-已拒绝
     */
    private Integer status;

    /**
     * 店铺类型：1-自营，2-第三方
     */
    private Integer shopType;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 店铺地址
     */
    private String address;

    /**
     * 营业执照号
     */
    private String businessLicense;

    /**
     * 法人代表
     */
    private String legalRepresentative;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

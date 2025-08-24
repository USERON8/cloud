package com.cloud.common.domain.dto.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家店铺信息DTO
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
     * 店铺头像URL
     */
    private String avatarUrl;

    /**
     * 店铺头像文件名
     */
    private String avatarFileName;

    /**
     * 店铺描述
     */
    private String description;

    /**
     * 客服电话
     */
    private String contactPhone;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 状态：0-关闭，1-营业，2-暂停营业
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
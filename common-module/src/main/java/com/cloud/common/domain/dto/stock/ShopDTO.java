package com.cloud.common.domain.dto.stock;

import lombok.Data;

/**
 * 店铺DTO
 */
@Data
public class ShopDTO {
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
    private String name;

    /**
     * 0-关闭，1-营业
     */
    private Integer status;
}
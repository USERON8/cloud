package com.cloud.stock.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存分页查询DTO
 *
 * @author cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "库存分页查询参数")
public class StockPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    private Long current = 1L;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数")
    private Long size = 10L;

    /**
     * 商品名称（模糊查询）
     */
    @Schema(description = "商品名称")
    private String productName;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    private Long productId;
}
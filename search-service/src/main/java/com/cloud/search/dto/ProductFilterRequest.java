package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品筛选请求参数 - 简化版
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Schema(description = "商品筛选请求参数")
public class ProductFilterRequest {

    /**
     * 搜索关键字
     */
    @Schema(description = "搜索关键字", example = "手机")
    private String keyword;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /**
     * 品牌ID
     */
    @Schema(description = "品牌ID", example = "1")
    private Long brandId;

    /**
     * 店铺ID
     */
    @Schema(description = "店铺ID", example = "1")
    private Long shopId;

    /**
     * 最低价格
     */
    @Schema(description = "最低价格", example = "1000.00")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    @Schema(description = "最高价格", example = "5000.00")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal maxPrice;

    /**
     * 最低销量
     */
    @Schema(description = "最低销量", example = "100")
    @Min(value = 0, message = "销量不能为负数")
    private Integer minSalesCount;

    /**
     * 是否推荐
     */
    @Schema(description = "是否推荐", example = "true")
    private Boolean recommended;

    /**
     * 是否新品
     */
    @Schema(description = "是否新品", example = "true")
    private Boolean isNew;

    /**
     * 是否热销
     */
    @Schema(description = "是否热销", example = "true")
    private Boolean isHot;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "price",
            allowableValues = {"price", "salesCount", "rating", "createdAt", "hotScore"})
    private String sortBy = "hotScore";

    /**
     * 排序方式
     */
    @Schema(description = "排序方式", example = "desc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    /**
     * 页码（从0开始）
     */
    @Schema(description = "页码", example = "0")
    @Min(value = 0, message = "页码不能为负数")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小至少为1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;
}

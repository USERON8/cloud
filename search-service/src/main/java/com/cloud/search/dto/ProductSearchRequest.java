package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索请求参数
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Schema(description = "商品搜索请求参数")
public class ProductSearchRequest {

    /**
     * 搜索关键字（商品名称、描述、标签）
     */
    @Schema(description = "搜索关键字", example = "智能手机")
    private String keyword;

    /**
     * 店铺ID过滤
     */
    @Schema(description = "店铺ID", example = "1")
    private Long shopId;

    /**
     * 店铺名称过滤
     */
    @Schema(description = "店铺名称", example = "华为官方旗舰店")
    private String shopName;

    /**
     * 分类ID过滤
     */
    @Schema(description = "分类ID", example = "3")
    private Long categoryId;

    /**
     * 分类名称过滤
     */
    @Schema(description = "分类名称", example = "手机")
    private String categoryName;

    /**
     * 品牌ID过滤
     */
    @Schema(description = "品牌ID", example = "1")
    private Long brandId;

    /**
     * 品牌名称过滤
     */
    @Schema(description = "品牌名称", example = "华为")
    private String brandName;

    /**
     * 最低价格
     */
    @Schema(description = "最低价格", example = "1000.00")
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    @Schema(description = "最高价格", example = "5000.00")
    private BigDecimal maxPrice;

    /**
     * 商品状态过滤：0-下架，1-上架
     */
    @Schema(description = "商品状态：0-下架，1-上架", example = "1")
    private Integer status;

    /**
     * 库存状态过滤：0-无库存，1-有库存
     */
    @Schema(description = "库存状态：0-无库存，1-有库存", example = "1")
    private Integer stockStatus;

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
     * 商品标签过滤
     */
    @Schema(description = "商品标签", example = "5G,双卡")
    private List<String> tags;

    /**
     * 最低销量
     */
    @Schema(description = "最低销量", example = "100")
    private Integer minSalesCount;

    /**
     * 最低评分
     */
    @Schema(description = "最低评分", example = "4.0")
    private BigDecimal minRating;

    /**
     * 页码（从0开始）
     */
    @Schema(description = "页码", example = "0")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "price", allowableValues = {"price", "salesCount", "rating", "createdAt", "hotScore"})
    private String sortBy;

    /**
     * 排序方式
     */
    @Schema(description = "排序方式", example = "asc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    /**
     * 是否启用高亮
     */
    @Schema(description = "是否启用高亮", example = "true")
    private Boolean highlight = false;

    /**
     * 是否返回聚合信息
     */
    @Schema(description = "是否返回聚合信息", example = "true")
    private Boolean includeAggregations = false;
}

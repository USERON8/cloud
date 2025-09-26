package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺搜索请求参数
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Schema(description = "店铺搜索请求参数")
public class ShopSearchRequest {

    /**
     * 搜索关键字（店铺名称、描述、地址）
     */
    @Schema(description = "搜索关键字", example = "华为旗舰店")
    private String keyword;

    /**
     * 商家ID过滤
     */
    @Schema(description = "商家ID", example = "1")
    private Long merchantId;

    /**
     * 店铺状态过滤：0-关闭，1-营业
     */
    @Schema(description = "店铺状态：0-关闭，1-营业", example = "1")
    private Integer status;

    /**
     * 最低评分
     */
    @Schema(description = "最低评分", example = "4.0")
    private BigDecimal minRating;

    /**
     * 最低商品数量
     */
    @Schema(description = "最低商品数量", example = "10")
    private Integer minProductCount;

    /**
     * 最低关注数量
     */
    @Schema(description = "最低关注数量", example = "100")
    private Integer minFollowCount;

    /**
     * 是否推荐店铺
     */
    @Schema(description = "是否推荐店铺", example = "true")
    private Boolean recommended;

    /**
     * 地址关键字
     */
    @Schema(description = "地址关键字", example = "北京")
    private String addressKeyword;

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
    @Schema(description = "排序字段", example = "rating", allowableValues = {"rating", "productCount", "followCount", "createdAt", "hotScore"})
    private String sortBy;

    /**
     * 排序方式
     */
    @Schema(description = "排序方式", example = "desc", allowableValues = {"asc", "desc"})
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

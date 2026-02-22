package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Product search request")
public class ProductSearchRequest {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Shop id")
    private Long shopId;

    @Schema(description = "Shop name")
    private String shopName;

    @Schema(description = "Category id")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Brand id")
    private Long brandId;

    @Schema(description = "Brand name")
    private String brandName;

    @Schema(description = "Min price")
    private BigDecimal minPrice;

    @Schema(description = "Max price")
    private BigDecimal maxPrice;

    @Schema(description = "Product status")
    private Integer status;

    @Schema(description = "Stock status")
    private Integer stockStatus;

    @Schema(description = "Recommended")
    private Boolean recommended;

    @Schema(description = "New product")
    private Boolean isNew;

    @Schema(description = "Hot product")
    private Boolean isHot;

    @Schema(description = "Tags")
    private List<String> tags;

    @Schema(description = "Min sales count")
    private Integer minSalesCount;

    @Schema(description = "Min rating")
    private BigDecimal minRating;

    @Schema(description = "Page number")
    private Integer page = 0;

    @Schema(description = "Page size")
    private Integer size = 20;

    @Schema(description = "Sort by")
    private String sortBy;

    @Schema(description = "Sort order")
    private String sortOrder = "desc";

    @Schema(description = "Enable highlight")
    private Boolean highlight = false;

    @Schema(description = "Include aggregations")
    private Boolean includeAggregations = false;
}

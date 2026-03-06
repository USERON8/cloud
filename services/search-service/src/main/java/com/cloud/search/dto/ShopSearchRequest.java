package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Shop search request")
public class ShopSearchRequest {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Merchant id")
    private Long merchantId;

    @Schema(description = "Shop status")
    private Integer status;

    @Schema(description = "Min rating")
    private BigDecimal minRating;

    @Schema(description = "Min product count")
    private Integer minProductCount;

    @Schema(description = "Min follow count")
    private Integer minFollowCount;

    @Schema(description = "Recommended")
    private Boolean recommended;

    @Schema(description = "Address keyword")
    private String addressKeyword;

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

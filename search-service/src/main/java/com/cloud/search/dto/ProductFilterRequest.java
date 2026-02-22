package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Product filter request")
public class ProductFilterRequest {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Category id")
    private Long categoryId;

    @Schema(description = "Brand id")
    private Long brandId;

    @Schema(description = "Shop id")
    private Long shopId;

    @Schema(description = "Min price")
    @DecimalMin(value = "0", message = "Min price must be greater than or equal to 0")
    private BigDecimal minPrice;

    @Schema(description = "Max price")
    @DecimalMin(value = "0", message = "Max price must be greater than or equal to 0")
    private BigDecimal maxPrice;

    @Schema(description = "Min sales count")
    @Min(value = 0, message = "Min sales count must be greater than or equal to 0")
    private Integer minSalesCount;

    @Schema(description = "Recommended")
    private Boolean recommended;

    @Schema(description = "New product")
    private Boolean isNew;

    @Schema(description = "Hot product")
    private Boolean isHot;

    @Schema(description = "Sort by")
    private String sortBy = "hotScore";

    @Schema(description = "Sort order")
    private String sortOrder = "desc";

    @Schema(description = "Page number")
    @Min(value = 0, message = "Page number must be greater than or equal to 0")
    private Integer page = 0;

    @Schema(description = "Page size")
    @Min(value = 1, message = "Page size must be greater than or equal to 1")
    @Max(value = 100, message = "Page size must be less than or equal to 100")
    private Integer size = 20;
}

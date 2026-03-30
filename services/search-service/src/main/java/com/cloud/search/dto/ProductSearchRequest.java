package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

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
  @DecimalMin(
      value = "0.0",
      inclusive = true,
      message = "minPrice must be greater than or equal to 0")
  private BigDecimal minPrice;

  @Schema(description = "Max price")
  @DecimalMin(
      value = "0.0",
      inclusive = true,
      message = "maxPrice must be greater than or equal to 0")
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
  @Min(value = 0, message = "minSalesCount must be greater than or equal to 0")
  private Integer minSalesCount;

  @Schema(description = "Min rating")
  @DecimalMin(
      value = "0.0",
      inclusive = true,
      message = "minRating must be greater than or equal to 0")
  private BigDecimal minRating;

  @Schema(description = "Page number")
  @Min(value = 0, message = "page must be greater than or equal to 0")
  private Integer page = 0;

  @Schema(description = "Page size")
  @Min(value = 1, message = "size must be greater than 0")
  @Max(value = 100, message = "size must be less than or equal to 100")
  private Integer size = 20;

  @Schema(description = "Sort by")
  private String sortBy;

  @Schema(description = "Sort order")
  @Pattern(regexp = "(?i)asc|desc", message = "sortOrder must be asc or desc")
  private String sortOrder = "desc";

  @Schema(description = "Enable highlight")
  private Boolean highlight = false;

  @Schema(description = "Include aggregations")
  private Boolean includeAggregations = false;

  @AssertTrue(message = "maxPrice must be greater than or equal to minPrice")
  public boolean isPriceRangeValid() {
    if (minPrice == null || maxPrice == null) {
      return true;
    }
    return maxPrice.compareTo(minPrice) >= 0;
  }
}

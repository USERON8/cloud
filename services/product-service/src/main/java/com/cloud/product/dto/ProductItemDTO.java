package com.cloud.product.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductItemDTO {
  private Long id;
  private Long shopId;
  private String name;
  private BigDecimal price;
  private Integer stockQuantity;
  private Long categoryId;
  private Long brandId;
  private Integer status;
  private String description;
  private String imageUrl;
}

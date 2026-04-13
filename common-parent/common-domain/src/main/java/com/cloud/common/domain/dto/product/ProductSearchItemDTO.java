package com.cloud.common.domain.dto.product;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductSearchItemDTO implements Serializable {
  private static final long serialVersionUID = 1L;

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

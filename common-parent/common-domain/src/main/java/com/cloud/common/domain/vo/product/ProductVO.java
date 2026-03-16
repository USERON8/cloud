package com.cloud.common.domain.vo.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductVO {

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

  private String imageFile;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

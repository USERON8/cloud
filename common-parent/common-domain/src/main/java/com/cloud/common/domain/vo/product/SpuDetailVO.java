package com.cloud.common.domain.vo.product;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SpuDetailVO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long spuId;
  private String spuName;
  private String subtitle;
  private Long categoryId;
  private String categoryName;
  private Long brandId;
  private String brandName;
  private Long merchantId;
  private Integer status;
  private String description;
  private String mainImage;
  private String mainImageFile;
  private String tags;
  private BigDecimal rating;
  private Integer reviewCount;
  private Boolean recommended;
  private Boolean isHot;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<SkuDetailVO> skus;
}

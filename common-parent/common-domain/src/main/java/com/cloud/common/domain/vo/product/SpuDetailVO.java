package com.cloud.common.domain.vo.product;

import java.io.Serial;
import java.io.Serializable;
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
  private Long brandId;
  private Long merchantId;
  private Integer status;
  private String description;
  private String mainImage;
  private String mainImageFile;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<SkuDetailVO> skus;
}

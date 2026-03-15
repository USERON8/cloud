package com.cloud.common.domain.vo.product;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkuDetailVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Long spuId;
    private String skuCode;
    private String skuName;
    private String specJson;
    private BigDecimal salePrice;
    private BigDecimal marketPrice;
    private BigDecimal costPrice;
    private Integer status;
    private String imageUrl;
    private String imageFile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

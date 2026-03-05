package com.cloud.common.domain.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SkuDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long skuId;

    @NotBlank
    private String skuCode;

    @NotBlank
    private String skuName;

    private String specJson;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    private BigDecimal marketPrice;

    private BigDecimal costPrice;

    private Integer status = 1;

    private String imageUrl;
}

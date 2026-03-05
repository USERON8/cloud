package com.cloud.common.domain.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpuDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long spuId;

    @NotBlank
    private String spuName;

    private String subtitle;

    @NotNull
    private Long categoryId;

    private Long brandId;

    @NotNull
    private Long merchantId;

    private Integer status = 1;

    private String description;

    private String mainImage;
}

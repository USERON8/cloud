package com.cloud.common.domain.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StockOperateCommandDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String subOrderNo;

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String reason;
}

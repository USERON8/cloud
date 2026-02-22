package com.cloud.common.domain.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockRequestDTO {

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    private String productName;

    @NotNull(message = "Stock quantity cannot be null")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Frozen quantity cannot be negative")
    private Integer frozenQuantity = 0;

    private Integer stockStatus = 1;
}

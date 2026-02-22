package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "Simple order create request")
public class SimpleOrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Product ID", example = "1001")
    @NotNull(message = "productId is required")
    private Long productId;

    @Schema(description = "Product name", example = "Apple iPhone 15 Pro")
    private String productName;

    @Schema(description = "Product unit price", example = "8999.00")
    @NotNull(message = "productPrice is required")
    @Positive(message = "productPrice must be greater than 0")
    private BigDecimal productPrice;

    @Schema(description = "Quantity", example = "1")
    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;

    @Schema(description = "Address ID", example = "1001")
    private Long addressId;

    @Schema(description = "Order remark", example = "Please call before delivery")
    private String remark;

    public BigDecimal getTotalAmount() {
        if (productPrice != null && quantity != null) {
            return productPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
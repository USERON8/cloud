package com.cloud.common.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Order item DTO")
public class OrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Order item ID")
    private Long id;

    @Schema(description = "Order ID")
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    @Schema(description = "Product ID")
    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @Schema(description = "Product snapshot")
    @NotBlank(message = "Product snapshot cannot be blank")
    private String productSnapshot;

    @Schema(description = "Purchase quantity")
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @Schema(description = "Unit price at purchase time")
    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    @Schema(description = "Created by")
    private Long createBy;

    @Schema(description = "Updated by")
    private Long updateBy;

    @Schema(description = "Optimistic lock version")
    private Integer version;

    @Schema(description = "Soft delete flag")
    private Integer deleted;
}

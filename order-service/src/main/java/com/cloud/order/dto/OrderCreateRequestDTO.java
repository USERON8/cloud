package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order create request")
public class OrderCreateRequestDTO {

    @NotNull(message = "userId is required")
    @Schema(description = "User ID", example = "123456")
    private Long userId;

    @NotNull(message = "addressId is required")
    @Schema(description = "Address ID", example = "789")
    private Long addressId;

    @DecimalMin(value = "0.01", message = "totalAmount must be greater than 0")
    @Schema(description = "Total amount", example = "99.90")
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.01", message = "payAmount must be greater than 0")
    @Schema(description = "Pay amount", example = "89.90")
    private BigDecimal payAmount;

    @NotEmpty(message = "orderItems cannot be empty")
    @Valid
    @Schema(description = "Order items")
    private List<OrderItemCreateDTO> orderItems;

    @Schema(description = "Order remark", example = "Please deliver in daytime")
    private String remark;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Order item create request")
    public static class OrderItemCreateDTO {

        @NotNull(message = "productId is required")
        @Schema(description = "Product ID", example = "456789")
        private Long productId;

        @NotBlank(message = "productName is required")
        @Schema(description = "Product name", example = "Apple iPhone 15")
        private String productName;

        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        @Schema(description = "Unit price", example = "6999.00")
        private BigDecimal price;

        @Min(value = 1, message = "quantity must be at least 1")
        @Schema(description = "Quantity", example = "2")
        private Integer quantity;

        @Schema(description = "Specification", example = "256GB Black")
        private String specification;

        @Schema(description = "Product image URL", example = "https://example.com/product.jpg")
        private String imageUrl;
    }
}
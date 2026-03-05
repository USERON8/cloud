package com.cloud.common.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Order create DTO")
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    @NotNull(message = "userId cannot be null")
    private Long userId;

    @Schema(description = "Shop ID")
    @NotNull(message = "shopId cannot be null")
    private Long shopId;

    @Schema(description = "Receiver name")
    private String receiverName;

    @Schema(description = "Receiver phone")
    private String receiverPhone;

    @Schema(description = "Receiver address")
    private String receiverAddress;

    @Schema(description = "Pay type")
    private Integer payType;

    @Schema(description = "Total amount")
    private BigDecimal totalAmount;

    @Schema(description = "Pay amount")
    private BigDecimal payAmount;

    @Schema(description = "Address ID")
    private Long addressId;

    @Schema(description = "Discount amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Schema(description = "Shipping fee")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Schema(description = "Remark")
    @Size(max = 255, message = "remark length must be less than or equal to 255")
    private String remark;

    @Schema(description = "Order items")
    @NotEmpty(message = "orderItems cannot be empty")
    private List<OrderItemDTO> orderItems;

    @Data
    @Schema(description = "Order item DTO")
    public static class OrderItemDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Product ID")
        @NotNull(message = "productId cannot be null")
        private Long productId;

        @Schema(description = "Product name")
        @NotBlank(message = "productName cannot be blank")
        private String productName;

        @Schema(description = "Product image")
        private String productImage;

        @Schema(description = "Unit price")
        @NotNull(message = "price cannot be null")
        @Positive(message = "price must be positive")
        private BigDecimal price;

        @Schema(description = "Original product price")
        private BigDecimal productPrice;

        @Schema(description = "Quantity")
        @NotNull(message = "quantity cannot be null")
        @Positive(message = "quantity must be positive")
        private Integer quantity;

        @Schema(description = "Product snapshot")
        private Object productSnapshot;

        @Schema(description = "Product specs")
        private String productSpecs;
    }
}

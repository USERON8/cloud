package com.cloud.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateMainOrderRequest {
    @NotNull
    private Long userId;

    private Long cartId;

    @DecimalMin("0.00")
    private BigDecimal totalAmount;

    @DecimalMin("0.00")
    private BigDecimal payableAmount;

    private String remark;

    private String idempotencyKey;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    @Valid
    private List<CreateSubOrderRequest> subOrders;

    @AssertTrue(message = "cartId or subOrders is required")
    public boolean isOrderSourceValid() {
        return cartId != null || (subOrders != null && !subOrders.isEmpty());
    }

    @Data
    public static class CreateSubOrderRequest {
        @NotNull
        private Long merchantId;

        @DecimalMin("0.00")
        private BigDecimal itemAmount;

        @DecimalMin("0.00")
        private BigDecimal shippingFee;

        @DecimalMin("0.00")
        private BigDecimal discountAmount;

        @DecimalMin("0.00")
        private BigDecimal payableAmount;

        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;

        @Valid
        @NotEmpty
        private List<CreateOrderItemRequest> items;
    }

    @Data
    public static class CreateOrderItemRequest {
        @NotNull
        private Long spuId;

        @NotNull
        private Long skuId;

        private String skuCode;
        private String skuName;
        private String skuSnapshot;

        @NotNull
        private Integer quantity;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal unitPrice;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal totalPrice;
    }
}

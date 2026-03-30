package com.cloud.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CreateMainOrderRequest {
  private Long userId;

  private Long cartId;

  private Long spuId;

  private Long skuId;

  private Integer quantity;

  @DecimalMin("0.00")
  private BigDecimal totalAmount;

  @DecimalMin("0.00")
  private BigDecimal payableAmount;

  private String remark;

  @NotBlank(message = "clientOrderId is required")
  @Schema(
      description = "Client-generated business order id",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String clientOrderId;

  @JsonIgnore
  @Schema(hidden = true)
  private String idempotencyKey;

  private String receiverName;

  private String receiverPhone;

  private String receiverAddress;

  @Valid
  @JsonIgnore
  @Schema(hidden = true)
  private List<CreateSubOrderRequest> subOrders;

  @AssertTrue(message = "cartId or single item (spuId, skuId, quantity) is required")
  public boolean isOrderSourceValid() {
    boolean cartOrder = cartId != null && spuId == null && skuId == null;
    boolean singleItemOrder =
        cartId == null && spuId != null && skuId != null && quantity != null && quantity > 0;
    return cartOrder || singleItemOrder;
  }

  @Data
  public static class CreateSubOrderRequest {
    @NotNull private Long merchantId;

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

    @Valid @NotEmpty private List<CreateOrderItemRequest> items;
  }

  @Data
  public static class CreateOrderItemRequest {
    @NotNull private Long spuId;

    @NotNull private Long skuId;

    private String skuCode;
    private String skuName;
    private String skuSnapshot;

    @NotNull private Integer quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalPrice;
  }
}

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
@Schema(description = "Create main order request")
public class CreateMainOrderRequest {
  @Schema(description = "User id, required for privileged creation")
  private Long userId;

  @Schema(description = "Cart id for cart checkout")
  private Long cartId;

  @Schema(description = "Spu id for single item checkout")
  private Long spuId;

  @Schema(description = "Sku id for single item checkout")
  private Long skuId;

  @Schema(description = "Purchase quantity for single item checkout")
  private Integer quantity;

  @Schema(description = "Total order amount")
  @DecimalMin(value = "0.00", message = "totalAmount must be greater than or equal to 0")
  private BigDecimal totalAmount;

  @Schema(description = "Payable amount after discount")
  @DecimalMin(value = "0.00", message = "payableAmount must be greater than or equal to 0")
  private BigDecimal payableAmount;

  @Schema(description = "Order remark")
  private String remark;

  @NotBlank(message = "clientOrderId is required")
  @Schema(
      description = "Client-generated business order id",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String clientOrderId;

  @JsonIgnore
  @Schema(hidden = true)
  private String idempotencyKey;

  @Schema(description = "Receiver name")
  private String receiverName;

  @Schema(description = "Receiver phone")
  private String receiverPhone;

  @Schema(description = "Receiver address")
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
  @Schema(description = "Create sub-order request")
  public static class CreateSubOrderRequest {
    @Schema(description = "Merchant id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "merchantId is required")
    private Long merchantId;

    @Schema(description = "Sub-order item amount")
    @DecimalMin(value = "0.00", message = "itemAmount must be greater than or equal to 0")
    private BigDecimal itemAmount;

    @Schema(description = "Shipping fee")
    @DecimalMin(value = "0.00", message = "shippingFee must be greater than or equal to 0")
    private BigDecimal shippingFee;

    @Schema(description = "Discount amount")
    @DecimalMin(value = "0.00", message = "discountAmount must be greater than or equal to 0")
    private BigDecimal discountAmount;

    @Schema(description = "Payable amount")
    @DecimalMin(value = "0.00", message = "payableAmount must be greater than or equal to 0")
    private BigDecimal payableAmount;

    @Schema(description = "Receiver name")
    private String receiverName;

    @Schema(description = "Receiver phone")
    private String receiverPhone;

    @Schema(description = "Receiver address")
    private String receiverAddress;

    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<CreateOrderItemRequest> items;
  }

  @Data
  @Schema(description = "Create order item request")
  public static class CreateOrderItemRequest {
    @Schema(description = "Spu id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "spuId is required")
    private Long spuId;

    @Schema(description = "Sku id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "skuId is required")
    private Long skuId;

    @Schema(description = "Sku code")
    private String skuCode;

    @Schema(description = "Sku name")
    private String skuName;

    @Schema(description = "Sku snapshot")
    private String skuSnapshot;

    @Schema(description = "Quantity", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "quantity is required")
    private Integer quantity;

    @NotNull
    @Schema(description = "Unit price", requiredMode = Schema.RequiredMode.REQUIRED)
    @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
    private BigDecimal unitPrice;

    @NotNull
    @Schema(description = "Total price", requiredMode = Schema.RequiredMode.REQUIRED)
    @DecimalMin(value = "0.00", message = "totalPrice must be greater than or equal to 0")
    private BigDecimal totalPrice;
  }
}

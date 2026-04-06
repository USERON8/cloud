package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Sync current cart request")
public class CartSyncRequest {

  @Valid
  @Schema(description = "Current cart items")
  private List<CartSyncItemRequest> items = List.of();

  @Data
  @Schema(description = "Cart item payload")
  public static class CartSyncItemRequest {
    @NotNull(message = "spuId is required")
    private Long spuId;

    @NotNull(message = "skuId is required")
    private Long skuId;

    @NotBlank(message = "skuName is required")
    private String skuName;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
    private BigDecimal unitPrice;

    @NotNull(message = "quantity is required")
    private Integer quantity;

    private Integer selected;

    private Long shopId;
  }
}

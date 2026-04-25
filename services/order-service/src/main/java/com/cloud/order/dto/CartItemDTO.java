package com.cloud.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "Cart item")
public class CartItemDTO {
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Long id;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Long cartId;

  private Long spuId;
  private Long skuId;
  private String skuName;
  private BigDecimal unitPrice;
  private Integer quantity;
  private Integer selected;
  private Integer checkedOut;
  private Long shopId;
  private String productName;
}

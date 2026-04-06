package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Cart")
public class CartDTO {
  private Long id;
  private String cartNo;
  private Long userId;
  private String cartStatus;
  private Integer selectedCount;
  private BigDecimal totalAmount;
  private List<CartItemDTO> items;
}

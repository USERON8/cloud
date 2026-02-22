package com.cloud.order.dto;

import com.cloud.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Order page query")
public class OrderPageQueryDTO extends PageQuery {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Order status: 0 pending payment, 1 paid, 2 shipped, 3 completed, 4 cancelled")
    private Integer status;

    @Schema(description = "Minimum order amount")
    private BigDecimal minAmount;

    @Schema(description = "Maximum order amount")
    private BigDecimal maxAmount;
}
package com.cloud.order.dto;

import com.cloud.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单分页查询DTO
 *
 * @author cloud
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单分页查询DTO")
public class OrderPageQueryDTO extends PageQuery {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消")
    private Integer status;

    /**
     * 最小订单金额
     */
    @Schema(description = "最小订单金额")
    private BigDecimal minAmount;

    /**
     * 最大订单金额
     */
    @Schema(description = "最大订单金额")
    private BigDecimal maxAmount;
}

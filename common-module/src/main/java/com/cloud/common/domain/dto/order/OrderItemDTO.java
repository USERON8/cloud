package com.cloud.common.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项DTO
 * 与数据库表order_item字段完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Schema(description = "订单项DTO")
public class OrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单项ID - 对应数据库字段: id
     */
    @Schema(description = "订单项ID")
    private Long id;

    /**
     * 订单ID - 对应数据库字段: order_id
     */
    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 商品ID - 对应数据库字段: product_id
     */
    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品快照 - 对应数据库字段: product_snapshot
     * 存储下单时的商品信息JSON
     */
    @Schema(description = "商品快照")
    @NotBlank(message = "商品快照不能为空")
    private String productSnapshot;

    /**
     * 购买数量 - 对应数据库字段: quantity
     */
    @Schema(description = "购买数量")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 购买时单价 - 对应数据库字段: price
     */
    @Schema(description = "购买时单价")
    @NotNull(message = "购买时单价不能为空")
    @DecimalMin(value = "0.01", message = "购买时单价必须大于0")
    private BigDecimal price;

    /**
     * 创建时间 - 对应数据库字段: create_time
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间 - 对应数据库字段: update_time
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 创建人 - 对应数据库字段: create_by
     */
    @Schema(description = "创建人")
    private Long createBy;

    /**
     * 更新人 - 对应数据库字段: update_by
     */
    @Schema(description = "更新人")
    private Long updateBy;

    /**
     * 乐观锁版本号 - 对应数据库字段: version
     */
    @Schema(description = "乐观锁版本号")
    private Integer version;

    /**
     * 软删除标记 - 对应数据库字段: deleted
     * 0-未删除，1-已删除
     */
    @Schema(description = "软删除标记")
    private Integer deleted;
}

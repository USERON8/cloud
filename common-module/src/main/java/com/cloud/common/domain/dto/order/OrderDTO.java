package com.cloud.common.domain.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单DTO
 * 与数据库表orders字段完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Schema(description = "订单DTO")
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID - 对应数据库字段: id
     */
    @Schema(description = "订单ID")
    private Long id;

    /**
     * 订单号 - 对应数据库字段: order_no
     */
    @Schema(description = "订单号")
    @Size(max = 32, message = "订单号长度不能超过32个字符")
    private String orderNo;

    /**
     * 用户ID - 对应数据库字段: user_id
     */
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单总额 - 对应数据库字段: total_amount
     */
    @Schema(description = "订单总额")
    @NotNull(message = "订单总额不能为空")
    @DecimalMin(value = "0.01", message = "订单总额必须大于0")
    private BigDecimal totalAmount;

    /**
     * 实付金额 - 对应数据库字段: pay_amount
     */
    @Schema(description = "实付金额")
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.01", message = "实付金额必须大于0")
    private BigDecimal payAmount;

    /**
     * 订单状态 - 对应数据库字段: status
     * 0-待支付，1-已支付，2-已发货，3-已完成，4-已取消
     */
    @Schema(description = "订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消")
    @Min(value = 0, message = "订单状态值不能小于0")
    @Max(value = 4, message = "订单状态值不能大于4")
    private Integer status;

    /**
     * 地址ID - 对应数据库字段: address_id
     */
    @Schema(description = "地址ID")
    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    /**
     * 支付时间 - 对应数据库字段: pay_time
     */
    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    /**
     * 发货时间 - 对应数据库字段: ship_time
     */
    @Schema(description = "发货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;

    /**
     * 完成时间 - 对应数据库字段: complete_time
     */
    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    /**
     * 取消时间 - 对应数据库字段: cancel_time
     */
    @Schema(description = "取消时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    /**
     * 取消原因 - 对应数据库字段: cancel_reason
     */
    @Schema(description = "取消原因")
    @Size(max = 255, message = "取消原因长度不能超过255个字符")
    private String cancelReason;

    /**
     * 备注 - 对应数据库字段: remark
     */
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /**
     * 创建时间 - 对应数据库字段: create_time
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间 - 对应数据库字段: update_time
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    /**
     * 订单项列表 (关联数据，非数据库字段)
     */
    @Schema(description = "订单项列表")
    private List<OrderItemDTO> orderItems;
}

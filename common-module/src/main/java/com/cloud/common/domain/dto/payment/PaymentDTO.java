package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付DTO
 * 与数据库表payment字段完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class PaymentDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付ID - 对应数据库字段: id
     */
    private Long id;

    /**
     * 订单ID - 对应数据库字段: order_id
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 订单号 - 对应数据库字段: order_no
     */
    @Size(max = 32, message = "订单号长度不能超过32个字符")
    private String orderNo;

    /**
     * 用户ID - 对应数据库字段: user_id
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 支付金额 - 对应数据库字段: amount
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;

    /**
     * 状态 - 对应数据库字段: status
     * 0-待支付，1-成功，2-失败，3-已退款
     */
    @NotNull(message = "支付状态不能为空")
    @Min(value = 0, message = "支付状态值不能小于0")
    @Max(value = 3, message = "支付状态值不能大于3")
    private Integer status;

    /**
     * 渠道 - 对应数据库字段: channel
     * 1-支付宝，2-微信，3-银行卡
     */
    @NotNull(message = "支付渠道不能为空")
    @Min(value = 1, message = "支付渠道值不能小于1")
    @Max(value = 3, message = "支付渠道值不能大于3")
    private Integer channel;

    /**
     * 支付方式 - 扩展字段
     * ALIPAY-支付宝，WECHAT-微信，BANK_CARD-银行卡
     */
    @Size(max = 32, message = "支付方式长度不能超过32个字符")
    private String paymentMethod;

    /**
     * 第三方流水号 - 对应数据库字段: transaction_id
     */
    @Size(max = 100, message = "第三方流水号长度不能超过100个字符")
    private String transactionId;

    /**
     * 跟踪ID - 对应数据库字段: trace_id
     */
    @Size(max = 64, message = "跟踪ID长度不能超过64个字符")
    private String traceId;

    /**
     * 创建时间 - 对应数据库字段: created_at
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 对应数据库字段: updated_at
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除标记 - 对应数据库字段: deleted
     * 0-未删除，1-已删除
     */
    private Integer deleted;
}
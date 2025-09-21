package com.cloud.common.domain.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单DTO
 * 用于服务间调用传输
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
@Schema(description = "订单DTO")
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @Schema(description = "订单ID")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderNo;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 店铺ID
     */
    @Schema(description = "店铺ID")
    private Long shopId;

    /**
     * 店铺名称
     */
    @Schema(description = "店铺名称")
    private String shopName;

    /**
     * 订单总金额
     */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    /**
     * 优惠金额
     */
    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    @Schema(description = "运费")
    private BigDecimal shippingFee;

    /**
     * 订单状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消，5-已退款
     */
    @Schema(description = "订单状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消，5-已退款")
    private Integer status;

    /**
     * 支付方式：1-微信支付，2-支付宝，3-银联支付
     */
    @Schema(description = "支付方式：1-微信支付，2-支付宝，3-银联支付")
    private Integer payType;

    /**
     * 支付状态：0-未支付，1-已支付，2-支付失败，3-已退款
     */
    @Schema(description = "支付状态：0-未支付，1-已支付，2-支付失败，3-已退款")
    private Integer payStatus;

    /**
     * 收货人姓名
     */
    @Schema(description = "收货人姓名")
    private String receiverName;

    /**
     * 收货人电话
     */
    @Schema(description = "收货人电话")
    private String receiverPhone;

    /**
     * 收货人地址
     */
    @Schema(description = "收货人地址")
    private String receiverAddress;

    /**
     * 订单备注
     */
    @Schema(description = "订单备注")
    private String remark;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    @Schema(description = "发货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    @Schema(description = "收货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    /**
     * 取消时间
     */
    @Schema(description = "取消时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 订单项列表
     */
    @Schema(description = "订单项列表")
    private List<OrderItemDTO> orderItems;
}

package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_sub")
public class OrderSub extends BaseEntity<OrderSub> {

    @TableField("sub_order_no")
    private String subOrderNo;

    @TableField("main_order_id")
    private Long mainOrderId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("order_status")
    private String orderStatus;

    @TableField("shipping_status")
    private String shippingStatus;

    @TableField("after_sale_status")
    private String afterSaleStatus;

    @TableField("item_amount")
    private BigDecimal itemAmount;

    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("shipping_company")
    private String shippingCompany;

    @TableField("tracking_number")
    private String trackingNumber;

    @TableField("shipped_at")
    private LocalDateTime shippedAt;

    @TableField("estimated_arrival")
    private LocalDate estimatedArrival;

    @TableField("received_at")
    private LocalDateTime receivedAt;

    @TableField("done_at")
    private LocalDateTime doneAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("close_reason")
    private String closeReason;
}

package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import com.cloud.order.enums.OrderStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "orders")
@Data
public class Order extends BaseEntity<Order> {

    


    @TableField(value = "order_no")
    private String orderNo;

    


    @TableField(value = "user_id")
    private Long userId;

    


    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    


    @TableField(value = "pay_amount")
    private BigDecimal payAmount;

    


    @TableField(value = "status")
    private Integer status;

    


    @TableField(value = "refund_status")
    private Integer refundStatus;

    


    @TableField(value = "address_id")
    private Long addressId;

    


    @TableField(value = "pay_time")
    private LocalDateTime payTime;

    


    @TableField(value = "ship_time")
    private LocalDateTime shipTime;

    


    @TableField(value = "complete_time")
    private LocalDateTime completeTime;

    


    @TableField(value = "cancel_time")
    private LocalDateTime cancelTime;

    


    @TableField(value = "cancel_reason")
    private String cancelReason;

    


    @TableField(value = "remark")
    private String remark;

    


    @TableField(value = "shop_id")
    private Long shopId;

    

    




    public static String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%03d", (int) (Math.random() * 1000));
    }

    




    public OrderStatusEnum getStatusEnum() {
        return OrderStatusEnum.fromCode(this.status);
    }

    




    public void setStatusEnum(OrderStatusEnum statusEnum) {
        this.status = statusEnum != null ? statusEnum.getCode() : null;
    }

    




    public boolean canPay() {
        return getStatusEnum() != null && getStatusEnum().canPay();
    }

    




    public boolean canCancel() {
        return getStatusEnum() != null && getStatusEnum().canCancel();
    }

    




    public boolean canShip() {
        return getStatusEnum() != null && getStatusEnum().canShip();
    }

    




    public boolean canComplete() {
        return getStatusEnum() != null && getStatusEnum().canComplete();
    }

    




    public boolean isFinalStatus() {
        return getStatusEnum() != null && getStatusEnum().isFinalStatus();
    }
}

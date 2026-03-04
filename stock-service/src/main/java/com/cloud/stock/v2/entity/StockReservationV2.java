package com.cloud.stock.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_reservation")
public class StockReservationV2 extends BaseEntity<StockReservationV2> {

    @TableField("reservation_no")
    private String reservationNo;

    @TableField("main_order_no")
    private String mainOrderNo;

    @TableField("sub_order_no")
    private String subOrderNo;

    @TableField("sku_id")
    private Long skuId;

    @TableField("reserved_qty")
    private Integer reservedQty;

    @TableField("reservation_status")
    private String reservationStatus;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("released_at")
    private LocalDateTime releasedAt;

    @TableField("released_reason")
    private String releasedReason;
}


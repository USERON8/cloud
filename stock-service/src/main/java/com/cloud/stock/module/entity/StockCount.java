package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 库存盘点表
 *
 * @author what's up
 * @TableName stock_count
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_count")
@Data
public class StockCount extends BaseEntity<StockCount> {
    /**
     * 盘点单号
     */
    @TableField(value = "count_no")
    private String countNo;

    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 预期库存数量（系统记录）
     */
    @TableField(value = "expected_quantity")
    private Integer expectedQuantity;

    /**
     * 实际库存数量（盘点结果）
     */
    @TableField(value = "actual_quantity")
    private Integer actualQuantity;

    /**
     * 库存差异（实际 - 预期）
     */
    @TableField(value = "difference")
    private Integer difference;

    /**
     * 盘点状态：PENDING-待确认，CONFIRMED-已确认，CANCELLED-已取消
     */
    @TableField(value = "status")
    private String status;

    /**
     * 盘点人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 盘点人名称
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 确认人ID
     */
    @TableField(value = "confirm_user_id")
    private Long confirmUserId;

    /**
     * 确认人名称
     */
    @TableField(value = "confirm_user_name")
    private String confirmUserName;

    /**
     * 盘点时间
     */
    @TableField(value = "count_time")
    private LocalDateTime countTime;

    /**
     * 确认时间
     */
    @TableField(value = "confirm_time")
    private LocalDateTime confirmTime;

    /**
     * 盘点备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 计算盘盈盘亏类型
     *
     * @return PROFIT-盘盈，LOSS-盘亏，BALANCE-平衡
     */
    public String getCountType() {
        if (difference == null || difference == 0) {
            return "BALANCE";
        }
        return difference > 0 ? "PROFIT" : "LOSS";
    }
}

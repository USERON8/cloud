package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_count")
@Data
public class StockCount extends BaseEntity<StockCount> {
    


    @TableField(value = "count_no")
    private String countNo;

    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "product_name")
    private String productName;

    


    @TableField(value = "expected_quantity")
    private Integer expectedQuantity;

    


    @TableField(value = "actual_quantity")
    private Integer actualQuantity;

    


    @TableField(value = "difference")
    private Integer difference;

    


    @TableField(value = "status")
    private String status;

    


    @TableField(value = "operator_id")
    private Long operatorId;

    


    @TableField(value = "operator_name")
    private String operatorName;

    


    @TableField(value = "confirm_user_id")
    private Long confirmUserId;

    


    @TableField(value = "confirm_user_name")
    private String confirmUserName;

    


    @TableField(value = "count_time")
    private LocalDateTime countTime;

    


    @TableField(value = "confirm_time")
    private LocalDateTime confirmTime;

    


    @TableField(value = "remark")
    private String remark;

    




    public String getCountType() {
        if (difference == null || difference == 0) {
            return "BALANCE";
        }
        return difference > 0 ? "PROFIT" : "LOSS";
    }
}

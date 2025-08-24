package com.cloud.merchant.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家操作日志表
 *
 * @TableName merchant_operation_log
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_operation_log")
@Data
public class MerchantOperationLog extends BaseEntity<MerchantOperationLog> {
    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 操作类型
     */
    @TableField(value = "operation")
    private String operation;

    /**
     * 操作描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 操作IP地址
     */
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 用户代理
     */
    @TableField(value = "user_agent")
    private String userAgent;
}
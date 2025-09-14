package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家结算账户表
 *
 * @author what's up
 * @TableName merchant_settlement_account
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_settlement_account")
@Data
public class MerchantSettlementAccount extends BaseEntity<MerchantSettlementAccount> {
    /**
     * 商家ID
     */
    @TableField(value = "merchant_id")
    private Long merchantId;

    /**
     * 账户名称
     */
    @TableField(value = "account_name")
    private String accountName;

    /**
     * 账户号码
     */
    @TableField(value = "account_number")
    private String accountNumber;

    /**
     * 账户类型：1-对公，2-对私
     */
    @TableField(value = "account_type")
    private Integer accountType;

    /**
     * 开户银行
     */
    @TableField(value = "bank_name")
    private String bankName;

    /**
     * 是否默认账户：0-否，1-是
     */
    @TableField(value = "is_default")
    private Integer isDefault;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status")
    private Integer status;
}
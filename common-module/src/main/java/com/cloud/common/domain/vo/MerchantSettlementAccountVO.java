package com.cloud.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家结算账户信息VO
 *
 * @author what's up
 */
@Data
public class MerchantSettlementAccountVO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 账户号码
     */
    private String accountNumber;

    /**
     * 账户类型：1-对公，2-对私
     */
    private Integer accountType;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 是否默认账户：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
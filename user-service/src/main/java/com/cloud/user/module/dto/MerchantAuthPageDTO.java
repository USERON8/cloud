package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户认证分页查询DTO
 *
 * @author what's up
 * @since 2025-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantAuthPageDTO extends PageQuery {

    /**
     * 商户ID
     */
    private Long merchantId;

    /**
     * 认证状态
     */
    private Integer authStatus;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 企业名称
     */
    private String companyName;
}

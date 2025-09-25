package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户分页查询DTO
 *
 * @author what's up
 * @since 2025-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPageDTO extends PageQuery {

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 商户状态
     */
    private Integer status;

    /**
     * 认证状态
     */
    private Integer authStatus;

    /**
     * 商户类型
     */
    private String merchantType;
}

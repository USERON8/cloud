package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPageDTO extends PageQuery {
    /**
     * 商家名称（模糊查询）
     */
    private String merchantName;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}
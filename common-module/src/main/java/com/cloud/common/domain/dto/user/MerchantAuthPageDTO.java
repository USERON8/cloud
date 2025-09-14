package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家认证分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantAuthPageDTO extends PageQuery {
    /**
     * 认证状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer authStatus;

    /**
     * 商家名称（模糊查询）
     */
    private String merchantName;
}
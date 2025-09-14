package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户地址分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserAddressPageDTO extends PageQuery {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收货人（模糊查询）
     */
    private String consignee;
}
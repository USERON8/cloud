package com.cloud.user.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户地址分页查询DTO
 *
 * @author what's up
 * @since 2025-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserAddressPageDTO extends PageQuery {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 收货人姓名
     */
    private String consignee;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 是否默认地址
     */
    private Integer isDefault;
}

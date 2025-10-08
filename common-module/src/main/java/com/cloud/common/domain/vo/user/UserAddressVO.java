package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户地址VO类
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class UserAddressVO {

    /**
     * 地址ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收货人
     */
    private String consignee;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 是否默认地址
     */
    private Boolean isDefault;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

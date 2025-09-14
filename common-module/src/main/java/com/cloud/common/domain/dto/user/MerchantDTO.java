package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家信息DTO
 */
@Data
public class MerchantDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 商家ID
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 商家名称
     */
    private String merchantName;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 用户类型
     */
    private String userType;
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    /**
     * 认证状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer authStatus;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标识
     */
    private Integer deleted;
}
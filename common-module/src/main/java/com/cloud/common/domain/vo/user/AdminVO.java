package com.cloud.common.domain.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员信息VO
 *
 * @author what's up
 */
@Data
public class AdminVO {
    /**
     * 管理员ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 角色
     */
    private String role;

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
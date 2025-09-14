package com.cloud.common.domain.dto.user;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPageDTO extends PageQuery {
    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 手机号（模糊查询）
     */
    private String phone;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 角色
     */
    private String role;
}
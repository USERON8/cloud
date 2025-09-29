package com.cloud.common.domain.event.user;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 用户变更日志事件
 * 
 * 记录用户相关的操作日志，包括：
 * - 用户注册、更新、删除
 * - 用户状态变更
 * - 用户登录、登出
 * - 用户权限变更等
 * 
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserChangeLogEvent extends BaseBusinessLogEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户状态变更
     */
    private String statusChange;

    /**
     * 登录IP地址
     */
    private String ipAddress;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 用户角色变更
     */
    private String roleChange;

    @Override
    public String getLogType() {
        return "USER_CHANGE";
    }
}

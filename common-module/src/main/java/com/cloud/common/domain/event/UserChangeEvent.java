package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户变更事件对象
 * 用于在服务间传递用户变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 变更前状态
     */
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    private Integer afterStatus;

    /**
     * 变更类型：1-创建用户，2-更新用户，3-删除用户，4-状态变更
     */
    private Integer changeType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;
}
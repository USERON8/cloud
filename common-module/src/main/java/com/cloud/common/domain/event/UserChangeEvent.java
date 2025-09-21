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
 * 精简版本，只包含核心信息，避免敏感数据传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID - 核心标识
     */
    private Long userId;

    /**
     * 事件类型
     * CREATED, UPDATED, DELETED, STATUS_CHANGED, LOGIN, LOGOUT
     */
    private String eventType;

    /**
     * 用户状态（当前状态）
     */
    private Integer status;

    /**
     * 操作时间
     */
    private LocalDateTime timestamp;

    /**
     * 事件追踪ID
     */
    private String traceId;

    /**
     * 可选的扩展数据（JSON格式）
     * 用于特定事件需要传递额外信息时使用
     */
    private String metadata;
}

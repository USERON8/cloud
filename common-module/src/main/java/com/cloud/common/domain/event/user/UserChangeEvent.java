package com.cloud.common.domain.event.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户变更事件对象
 * 
 * 标准字段设计：
 * - 核心标识: userId, eventType
 * - 状态变更: beforeStatus, afterStatus
 * - 追踪信息: timestamp, traceId
 * - 扩展信息: metadata, operator
 * 
 * @author CloudDevAgent
 * @since 2025-10-04
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
     * CREATED - 创建用户
     * UPDATED - 更新信息
     * DELETED - 删除用户
     * STATUS_CHANGED - 状态变更
     * LOGIN - 登录事件
     * LOGOUT - 登出事件
     */
    private String eventType;

    /**
     * 变更前状态
     * null表示新建操作
     */
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    private Integer afterStatus;

    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;

    /**
     * 分布式追踪ID (用于全链路追踪和幂等处理)
     */
    private String traceId;

    /**
     * 操作人标识
     * 可以是username或system
     */
    private String operator;

    /**
     * 扩展数据 (JSON格式)
     * 用于特定场景的额外信息，避免频繁修改事件结构
     * 
     * 示例:
     * - 登录事件: {"ip": "192.168.1.1", "device": "iPhone"}
     * - 信息更新: {"fields": ["nickname", "avatar"]}
     */
    private String metadata;
}

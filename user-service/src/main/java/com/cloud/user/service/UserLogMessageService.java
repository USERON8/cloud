package com.cloud.user.service;

/**
 * 用户日志消息服务类
 * 专门用于异步发送用户变更事件到RocketMQ，供日志服务消费并记录日志
 */
public interface UserLogMessageService {

    /**
     * 发送用户变更消息到RocketMQ
     *
     * @param userId        用户ID
     * @param username      用户名
     * @param beforeStatus  变更前状态
     * @param afterStatus   变更后状态
     * @param changeType    变更类型 1-创建用户 2-更新用户 3-删除用户 4-状态变更
     * @param operator      操作人
     */
    void sendUserChangeMessage(Long userId,
                               String username,
                               Integer beforeStatus,
                               Integer afterStatus,
                               Integer changeType,
                               String operator);
}
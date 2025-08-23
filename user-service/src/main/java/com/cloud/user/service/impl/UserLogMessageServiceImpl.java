package com.cloud.user.service.impl;

import com.cloud.common.domain.UserChangeEvent;
import com.cloud.user.service.UserLogMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户日志消息服务类
 * 专门用于异步发送用户变更事件到RocketMQ，供日志服务消费并记录日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogMessageServiceImpl implements UserLogMessageService {

    private final StreamBridge streamBridge;

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
    public void sendUserChangeMessage(Long userId,
                                      String username,
                                      Integer beforeStatus,
                                      Integer afterStatus,
                                      Integer changeType,
                                      String operator) {
        try {
            // 构建用户变更事件
            UserChangeEvent event = UserChangeEvent.builder()
                    .userId(userId)
                    .username(username)
                    .beforeStatus(beforeStatus)
                    .afterStatus(afterStatus)
                    .changeType(changeType)
                    .operator(operator)
                    .operateTime(LocalDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .build();

            // 通过StreamBridge发送消息到RocketMQ
            streamBridge.send("userLogProducer-out-0", event);
            log.info("发送用户变更消息成功，用户ID: {}, 变更类型: {}", userId, changeType);
        } catch (Exception e) {
            log.error("发送用户变更消息失败，用户ID: {}, 变更类型: {}", userId, changeType, e);
        }
    }
}
package com.cloud.log.handler;

import com.cloud.common.domain.event.UserChangeEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.log.document.UserLogDocument;
import com.cloud.log.repository.UserLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户事件消息处理器
 * 负责处理用户变更事件并存储到ES
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserEventMessageHandler {

    private final UserLogRepository userLogRepository;

    /**
     * 处理用户变更事件
     */
    public void handleUserChangeEvent(UserChangeEvent event, String traceId, String eventType) {
        try {
            // 幂等性检查
            if (isDuplicateEvent(traceId)) {
                log.warn("⚠️ 重复的用户事件消息，跳过处理 - TraceId: {}, 用户ID: {}", traceId, event.getUserId());
                return;
            }

            // 转换为ES文档
            UserLogDocument logDocument = convertToUserLogDocument(event, traceId, eventType);

            // 保存到ES
            UserLogDocument savedDocument = userLogRepository.save(logDocument);

            log.info("✅ 用户事件日志保存成功 - ES文档ID: {}, 用户ID: {}, 事件类型: {}, TraceId: {}",
                    savedDocument.getId(), event.getUserId(), eventType, traceId);

        } catch (Exception e) {
            log.error("❌ 处理用户事件消息失败 - 用户ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    event.getUserId(), eventType, traceId, e.getMessage(), e);
            throw new MessageConsumeException("处理用户事件消息失败", e);
        }
    }

    /**
     * 转换用户事件为ES日志文档
     */
    private UserLogDocument convertToUserLogDocument(UserChangeEvent event, String traceId, String eventType) {
        return UserLogDocument.builder()
                .id(UUID.randomUUID().toString())
                .traceId(traceId)
                .userId(event.getUserId())
                .username(event.getUsername())
                .nickname(event.getNickname())
                .phone(maskPhone(event.getPhone())) // 脱敏处理
                .userType(event.getUserType())
                .beforeStatus(event.getBeforeStatus())
                .afterStatus(event.getAfterStatus())
                .changeType(event.getChangeType())
                .changeTypeDesc(getChangeTypeDescription(event.getChangeType()))
                .operator(event.getOperator())
                .operateTime(event.getOperateTime())
                .logCreateTime(LocalDateTime.now())
                .sourceService("user-service")
                .eventType(eventType)
                .build();
    }

    /**
     * 检查是否为重复事件（幂等性处理）
     */
    private boolean isDuplicateEvent(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return false;
        }

        List<UserLogDocument> existingLogs = userLogRepository.findByTraceId(traceId);
        return !CollectionUtils.isEmpty(existingLogs);
    }

    /**
     * 手机号脱敏处理
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        // 保留前3位和后4位，中间用*代替
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 获取变更类型描述
     */
    private String getChangeTypeDescription(Integer changeType) {
        if (changeType == null) {
            return "未知操作";
        }

        return switch (changeType) {
            case 1 -> "创建用户";
            case 2 -> "更新用户";
            case 3 -> "删除用户";
            case 4 -> "状态变更";
            default -> "未知操作";
        };
    }
}

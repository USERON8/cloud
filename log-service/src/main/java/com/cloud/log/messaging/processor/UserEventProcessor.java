package com.cloud.log.messaging.processor;

import com.cloud.common.domain.event.UserChangeEvent;
import com.cloud.common.utils.MessageUtils;
import com.cloud.log.domain.document.UserEventDocument;
import com.cloud.log.service.UserEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户事件处理器
 * 负责处理用户事件并存储到Elasticsearch
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProcessor {

    private final UserEventService userEventService;

    /**
     * 处理用户事件
     */
    public void processUserEvent(UserChangeEvent event, MessageHeaders headers) {
        try {
            // 获取消息头信息
            String traceId = (String) headers.get("traceId");
            String eventType = (String) headers.get("eventType");
            String tag = (String) headers.get("rocketmq_TAGS");
            Long timestamp = (Long) headers.get("timestamp");

            // 记录消息消费日志
            MessageUtils.logMessageReceive("user-events", event, traceId);

            // 幂等性检查
            if (userEventService.existsByUserIdAndEventType(event.getUserId(), eventType, traceId)) {
                log.warn("⚠️ 用户事件已处理，跳过重复处理 - 用户ID: {}, 事件类型: {}, TraceId: {}",
                        event.getUserId(), eventType, traceId);
                return;
            }

            // 构建Elasticsearch文档
            UserEventDocument document = buildUserEventDocument(event, headers);

            // 敏感信息脱敏
            sanitizeSensitiveData(document);

            // 保存到Elasticsearch
            userEventService.saveUserEvent(document);

            log.info("📁 用户事件已存储到ES - 用户ID: {}, 事件类型: {}, 文档ID: {}, TraceId: {}",
                    event.getUserId(), eventType, document.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 处理用户事件失败 - 用户ID: {}, 错误: {}", event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("用户事件处理异常", e);
        }
    }

    /**
     * 构建用户事件文档
     */
    private UserEventDocument buildUserEventDocument(UserChangeEvent event, MessageHeaders headers) {
        String traceId = (String) headers.get("traceId");
        String eventType = (String) headers.get("eventType");
        String tag = (String) headers.get("rocketmq_TAGS");
        Long timestamp = (Long) headers.get("timestamp");

        return UserEventDocument.builder()
                .id(generateDocumentId(event.getUserId(), eventType, traceId))
                .userId(event.getUserId())
                .username("user_" + event.getUserId()) // 从metadata中提取或使用默认值
                .nickname(null) // UserChangeEvent中没有此字段
                .eventType(eventType)
                .tag(tag)
                .traceId(traceId)
                .phone(null) // UserChangeEvent中没有此字段
                .operatorName("system") // 默认操作人
                .eventTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .messageTimestamp(timestamp != null ? timestamp : System.currentTimeMillis())
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 敏感信息脱敏
     */
    private void sanitizeSensitiveData(UserEventDocument document) {
        // 手机号脱敏
        if (StringUtils.hasText(document.getPhone())) {
            document.setPhone(com.cloud.common.utils.StringUtils.maskPhone(document.getPhone()));
        }
    }

    /**
     * 生成文档ID
     */
    private String generateDocumentId(Long userId, String eventType, String traceId) {
        return com.cloud.common.utils.StringUtils.generateLogId("user_" + userId + "_" + eventType + "_" + traceId);
    }
}

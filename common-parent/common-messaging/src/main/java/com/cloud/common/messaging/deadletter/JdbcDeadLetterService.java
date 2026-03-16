package com.cloud.common.messaging.deadletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(JdbcTemplate.class)
public class JdbcDeadLetterService implements DeadLetterService {

    private static final int MAX_ERROR_LENGTH = 512;

    private static final String INSERT_SQL =
            "INSERT INTO dead_letter (topic, msg_id, payload, fail_reason, error_msg, status, service, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, 0, ?, NOW()) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "fail_reason = VALUES(fail_reason), "
                    + "error_msg = VALUES(error_msg), "
                    + "service = VALUES(service)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    public JdbcDeadLetterService(JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> mapperProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = mapperProvider.getIfAvailable();
    }

    @Override
    public void record(String topic, String msgId, String payload, DeadLetterReason reason, Throwable error) {
        String safeTopic = safe(topic);
        String safeMsgId = safe(msgId);
        String safePayload = payload == null ? "" : payload;
        String errorMsg = truncate(error == null ? null : error.getMessage());
        String reasonValue = reason == null ? DeadLetterReason.UNKNOWN.name() : reason.name();
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    safeTopic,
                    safeMsgId,
                    safePayload,
                    reasonValue,
                    errorMsg,
                    serviceName);
        } catch (Exception ex) {
            log.warn(
                    "Dead letter insert failed: topic={}, msgId={}, reason={}",
                    safeTopic,
                    safeMsgId,
                    reasonValue,
                    ex);
        }
    }

    @Override
    public void record(String topic, String msgId, Object payload, DeadLetterReason reason, Throwable error) {
        if (payload == null) {
            record(topic, msgId, "", reason, error);
            return;
        }
        if (payload instanceof String value) {
            record(topic, msgId, value, reason, error);
            return;
        }
        if (payload instanceof byte[] bytes) {
            record(topic, msgId, bytes, reason, error);
            return;
        }
        record(topic, msgId, serialize(payload), reason, error);
    }

    private String serialize(Object payload) {
        if (objectMapper == null) {
            return String.valueOf(payload);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Serialize dead letter payload failed", ex);
            return String.valueOf(payload);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private String safe(String value) {
        return Objects.requireNonNullElse(value, "");
    }
}

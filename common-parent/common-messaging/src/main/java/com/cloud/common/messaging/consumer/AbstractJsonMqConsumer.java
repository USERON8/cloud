package com.cloud.common.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@Slf4j
public abstract class AbstractJsonMqConsumer<T> extends AbstractMqConsumer<T> {

  @Autowired private ObjectMapper mqObjectMapper;

  @Override
  protected final T deserialize(byte[] body) {
    try {
      if (body == null) {
        return null;
      }
      Class<T> payloadClass = payloadClass();
      if (payloadClass != null) {
        return mqObjectMapper.readValue(body, payloadClass);
      }
      TypeReference<T> payloadTypeReference = payloadTypeReference();
      if (payloadTypeReference != null) {
        return mqObjectMapper.readValue(body, payloadTypeReference);
      }
      throw new IllegalStateException("No payload type configured for " + getClass().getName());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize " + payloadDescription(), ex);
    }
  }

  @Nullable
  protected Class<T> payloadClass() {
    return null;
  }

  @Nullable
  protected TypeReference<T> payloadTypeReference() {
    return null;
  }

  protected abstract String payloadDescription();

  protected final String resolveEventId(
      String eventType, @Nullable String explicitEventId, Object... identifiers) {
    if (StringUtils.hasText(explicitEventId)) {
      return explicitEventId.trim();
    }
    for (Object identifier : identifiers) {
      String text = asText(identifier);
      if (StringUtils.hasText(text)) {
        return eventType + ":" + text.trim();
      }
    }
    return eventType + ":" + System.currentTimeMillis();
  }

  protected final String readString(Map<String, Object> payload, String key) {
    if (payload == null || key == null) {
      return null;
    }
    return asText(payload.get(key));
  }

  protected final Long readLong(Map<String, Object> payload, String key) {
    if (payload == null || key == null) {
      return null;
    }
    Object value = payload.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      log.debug("Ignore non-long MQ payload field: key={}, value={}", key, value, ex);
      return null;
    }
  }

  protected final Boolean readBoolean(Map<String, Object> payload, String key) {
    if (payload == null || key == null) {
      return null;
    }
    Object value = payload.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }
    String text = String.valueOf(value);
    if (!StringUtils.hasText(text)) {
      return null;
    }
    return Boolean.parseBoolean(text);
  }

  protected final ObjectMapper objectMapper() {
    return mqObjectMapper;
  }

  private String asText(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}

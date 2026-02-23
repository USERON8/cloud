package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderContextStore {

    private static final String ORDER_CONTEXT_KEY_PREFIX = "payment:order:context:";
    private static final Duration ORDER_CONTEXT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveOrderContext(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderNo", event.getOrderNo());
            payload.put("productQuantityMap",
                    event.getProductQuantityMap() == null ? Map.of() : event.getProductQuantityMap());
            String value = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue().set(buildKey(event.getOrderId()), value, ORDER_CONTEXT_TTL);
        } catch (Exception ex) {
            log.warn("Save order context failed: orderId={}", event.getOrderId(), ex);
        }
    }

    public String getOrderNo(Long orderId) {
        Map<String, Object> payload = getPayload(orderId);
        Object value = payload.get("orderNo");
        return value == null ? null : String.valueOf(value);
    }

    public Map<Long, Integer> getProductQuantityMap(Long orderId) {
        Map<String, Object> payload = getPayload(orderId);
        Object value = payload.get("productQuantityMap");
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            try {
                Long productId = Long.parseLong(String.valueOf(entry.getKey()));
                Integer quantity = Integer.parseInt(String.valueOf(entry.getValue()));
                if (quantity > 0) {
                    result.put(productId, quantity);
                }
            } catch (Exception ignored) {
                // Skip invalid key/value pair from payload.
            }
        }
        return result;
    }

    private Map<String, Object> getPayload(Long orderId) {
        if (orderId == null) {
            return Map.of();
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(buildKey(orderId));
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("Read order context failed: orderId={}", orderId, ex);
            return Map.of();
        }
    }

    private String buildKey(Long orderId) {
        return ORDER_CONTEXT_KEY_PREFIX + orderId;
    }
}

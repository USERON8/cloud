package com.cloud.product.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheInvalidationMessageListener implements MessageListener {

    private final ProductCacheProtectionService productCacheProtectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            productCacheProtectionService.handleLocalCacheInvalidationMessage(payload);
        } catch (Exception e) {
            log.warn("Handle product cache pubsub message failed, payload={}", payload, e);
        }
    }
}

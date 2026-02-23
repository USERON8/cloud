package com.cloud.product.messaging;

import com.cloud.product.module.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchSyncProducer {

    private final StreamBridge streamBridge;
    @Value("${product.search.enabled:true}")
    private boolean searchSyncEnabled;

    public boolean sendProductCreated(Product product) {
        return sendProductUpsert(product, "PRODUCT_CREATED");
    }

    public boolean sendProductUpdated(Product product) {
        return sendProductUpsert(product, "PRODUCT_UPDATED");
    }

    public boolean sendProductDeleted(Long productId) {
        if (!searchSyncEnabled) {
            return true;
        }
        if (productId == null) {
            return false;
        }

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", "PRODUCT_DELETED");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("productId", productId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_KEYS, String.valueOf(productId));
        headers.put(MessageConst.PROPERTY_TAGS, "PRODUCT_DELETED");
        headers.put("eventId", eventId);
        headers.put("eventType", "PRODUCT_DELETED");

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(payload)
                .copyHeaders(headers)
                .build();

        return streamBridge.send("search-producer-out-0", message);
    }

    private boolean sendProductUpsert(Product product, String eventType) {
        if (!searchSyncEnabled) {
            return true;
        }
        if (product == null || product.getId() == null) {
            return false;
        }

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("productId", product.getId());
        payload.put("productName", product.getName());
        payload.put("price", product.getPrice());
        payload.put("stockQuantity", product.getStock());
        payload.put("categoryId", product.getCategoryId());
        payload.put("categoryName", product.getCategoryName());
        payload.put("brandId", product.getBrandId());
        payload.put("brandName", product.getBrandName());
        payload.put("status", product.getStatus());
        payload.put("description", product.getDescription());
        payload.put("imageUrl", product.getImageUrl());
        payload.put("shopId", product.getShopId());
        payload.put("shopName", product.getShopName());
        payload.put("salesCount", product.getSalesCount());
        payload.put("createdAt", toEpochMilli(product.getCreatedAt()));
        payload.put("updatedAt", toEpochMilli(product.getUpdatedAt()));

        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_KEYS, String.valueOf(product.getId()));
        headers.put(MessageConst.PROPERTY_TAGS, eventType);
        headers.put("eventId", eventId);
        headers.put("eventType", eventType);

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(payload)
                .copyHeaders(headers)
                .build();

        return streamBridge.send("search-producer-out-0", message);
    }

    private Long toEpochMilli(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}

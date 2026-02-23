package com.cloud.search.messaging;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncEventConsumer {

    private final ProductSearchService productSearchService;

    @Bean
    public Consumer<Message<Map<String, Object>>> searchConsumer() {
        return message -> {
            Map<String, Object> payload = message.getPayload();
            if (payload == null || payload.isEmpty()) {
                return;
            }

            String eventId = asString(payload.get("eventId"));
            if (!StringUtils.hasText(eventId)) {
                eventId = UUID.randomUUID().toString();
            }
            if (productSearchService.isEventProcessed(eventId)) {
                log.warn("Duplicate search sync event, skip: eventId={}", eventId);
                return;
            }

            try {
                String eventType = asString(payload.get("eventType"));
                if (!StringUtils.hasText(eventType)) {
                    log.warn("Missing eventType for search sync event: eventId={}", eventId);
                    return;
                }

                if ("PRODUCT_DELETED".equalsIgnoreCase(eventType)) {
                    Long productId = asLong(payload.get("productId"));
                    if (productId == null) {
                        log.warn("Missing productId for product delete event: eventId={}", eventId);
                        return;
                    }
                    productSearchService.deleteProduct(productId);
                    productSearchService.markEventProcessed(eventId);
                    return;
                }

                if (!"PRODUCT_CREATED".equalsIgnoreCase(eventType)
                        && !"PRODUCT_UPDATED".equalsIgnoreCase(eventType)) {
                    log.warn("Unsupported search sync eventType: eventType={}, eventId={}", eventType, eventId);
                    return;
                }

                ProductDocument document = buildProductDocument(payload);
                if (document.getProductId() == null) {
                    log.warn("Missing productId for upsert event: eventType={}, eventId={}", eventType, eventId);
                    return;
                }

                productSearchService.upsertProduct(document);
                productSearchService.markEventProcessed(eventId);
            } catch (Exception e) {
                log.error("Handle search sync event failed: eventId={}", eventId, e);
                throw new RuntimeException("Handle search sync event failed", e);
            }
        };
    }

    private ProductDocument buildProductDocument(Map<String, Object> payload) {
        Long productId = asLong(payload.get("productId"));
        String productName = asString(payload.get("productName"));
        String categoryName = asString(payload.get("categoryName"));
        String brandName = asString(payload.get("brandName"));
        String shopName = asString(payload.get("shopName"));

        return ProductDocument.builder()
                .id(productId == null ? null : String.valueOf(productId))
                .productId(productId)
                .productName(productName)
                .productNameKeyword(productName)
                .price(asBigDecimal(payload.get("price")))
                .stockQuantity(asInteger(payload.get("stockQuantity")))
                .categoryId(asLong(payload.get("categoryId")))
                .categoryName(categoryName)
                .categoryNameKeyword(categoryName)
                .brandId(asLong(payload.get("brandId")))
                .brandName(brandName)
                .brandNameKeyword(brandName)
                .status(asInteger(payload.get("status")))
                .description(asString(payload.get("description")))
                .imageUrl(asString(payload.get("imageUrl")))
                .shopId(asLong(payload.get("shopId")))
                .shopName(shopName)
                .salesCount(asInteger(payload.get("salesCount")))
                .createdAt(toLocalDateTime(payload.get("createdAt")))
                .updatedAt(toLocalDateTime(payload.get("updatedAt")))
                .recommended(asBoolean(payload.get("recommended")))
                .isNew(asBoolean(payload.get("isNew")))
                .isHot(asBoolean(payload.get("isHot")))
                .merchantId(asLong(payload.get("merchantId")))
                .merchantName(asString(payload.get("merchantName")))
                .remark(asString(payload.get("remark")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        Long epochMillis = asLong(value);
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}

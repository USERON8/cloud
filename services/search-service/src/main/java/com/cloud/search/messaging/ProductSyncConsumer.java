package com.cloud.search.messaging;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.ProductSyncEvent;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;



@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private static final String NS_PRODUCT_SYNC = "search:product:sync";

    private final MessageIdempotencyService messageIdempotencyService;
    private final ProductDocumentRepository productDocumentRepository;

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private ProductDubboApi productDubboApi;

    @Bean
    public Consumer<List<Message<ProductSyncEvent>>> productSyncConsumer() {
        return messages -> {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            List<ProductDocument> upserts = new ArrayList<>();
            List<String> deletes = new ArrayList<>();
            List<String> readyToMark = new ArrayList<>();
            Set<String> inFlight = new HashSet<>();

            try {
                for (Message<ProductSyncEvent> message : messages) {
                    if (message == null) {
                        continue;
                    }
                    ProductSyncEvent event = message.getPayload();
                    String eventId = resolveEventId(event);
                    if (!messageIdempotencyService.tryAcquire(NS_PRODUCT_SYNC, eventId)) {
                        log.warn("Duplicate product sync event, skip: eventId={}", eventId);
                        continue;
                    }
                    inFlight.add(eventId);

                    if (event == null || event.getSpuId() == null) {
                        readyToMark.add(eventId);
                        continue;
                    }

                    if ("PRODUCT_DELETE".equalsIgnoreCase(event.getEventType())) {
                        deletes.add(String.valueOf(event.getSpuId()));
                        readyToMark.add(eventId);
                        continue;
                    }

                    SpuDetailVO spu = productDubboApi.getSpuById(event.getSpuId());
                    if (spu == null) {
                        deletes.add(String.valueOf(event.getSpuId()));
                        readyToMark.add(eventId);
                        continue;
                    }

                    upserts.add(toDocument(spu));
                    readyToMark.add(eventId);
                }

                if (!deletes.isEmpty()) {
                    productDocumentRepository.deleteAllById(deletes);
                }
                if (!upserts.isEmpty()) {
                    productDocumentRepository.saveAll(upserts);
                }
                for (String eventId : readyToMark) {
                    messageIdempotencyService.markSuccess(NS_PRODUCT_SYNC, eventId);
                    inFlight.remove(eventId);
                }
            } catch (Exception ex) {
                for (String eventId : inFlight) {
                    messageIdempotencyService.release(NS_PRODUCT_SYNC, eventId);
                }
                log.error("Handle product sync batch failed", ex);
                throw new RuntimeException("Handle product sync failed", ex);
            }
        };
    }

    private ProductDocument toDocument(SpuDetailVO spu) {
        List<SkuDetailVO> skus = spu.getSkus();
        Optional<SkuDetailVO> minPriceSku = skus == null ? Optional.empty()
                : skus.stream()
                .filter(sku -> sku.getSalePrice() != null)
                .min(Comparator.comparing(SkuDetailVO::getSalePrice));

        BigDecimal price = minPriceSku.map(SkuDetailVO::getSalePrice).orElse(null);
        String skuCode = minPriceSku.map(SkuDetailVO::getSkuCode).orElse(null);
        String imageUrl = minPriceSku.map(SkuDetailVO::getImageUrl).orElse(spu.getMainImage());

        return ProductDocument.builder()
                .id(String.valueOf(spu.getSpuId()))
                .productId(spu.getSpuId())
                .productName(spu.getSpuName())
                .productNameKeyword(spu.getSpuName())
                .price(price)
                .categoryId(spu.getCategoryId())
                .brandId(spu.getBrandId())
                .merchantId(spu.getMerchantId())
                .status(spu.getStatus())
                .description(spu.getDescription())
                .imageUrl(imageUrl)
                .sku(skuCode)
                .createdAt(spu.getCreatedAt())
                .updatedAt(spu.getUpdatedAt())
                .build();
    }

    private String resolveEventId(ProductSyncEvent event) {
        if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        if (event != null && event.getSpuId() != null) {
            return "PRODUCT_SYNC:" + event.getSpuId();
        }
        return "PRODUCT_SYNC:" + System.currentTimeMillis();
    }
}

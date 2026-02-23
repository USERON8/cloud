package com.cloud.search.messaging;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.service.ProductSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSyncEventConsumerTest {

    @Mock
    private ProductSearchService productSearchService;

    private SearchSyncEventConsumer searchSyncEventConsumer;
    private Consumer<Message<Map<String, Object>>> consumer;

    @BeforeEach
    void setUp() {
        searchSyncEventConsumer = new SearchSyncEventConsumer(productSearchService);
        consumer = searchSyncEventConsumer.searchConsumer();
    }

    @Test
    void shouldUpsertAndMarkProcessedWhenProductCreatedEventReceived() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "evt-1");
        payload.put("eventType", "PRODUCT_CREATED");
        payload.put("productId", 1001L);
        payload.put("productName", "Phone");
        payload.put("price", new BigDecimal("1999.99"));
        payload.put("stockQuantity", 12);
        payload.put("status", 1);
        payload.put("createdAt", 1739923200000L);
        payload.put("updatedAt", 1739923200000L);

        when(productSearchService.isEventProcessed("evt-1")).thenReturn(false);

        consumer.accept(MessageBuilder.withPayload(payload).build());

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchService).upsertProduct(documentCaptor.capture());
        verify(productSearchService).markEventProcessed("evt-1");

        ProductDocument document = documentCaptor.getValue();
        assertThat(document.getProductId()).isEqualTo(1001L);
        assertThat(document.getProductName()).isEqualTo("Phone");
        assertThat(document.getPrice()).isEqualByComparingTo("1999.99");
    }

    @Test
    void shouldDeleteAndMarkProcessedWhenProductDeletedEventReceived() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "evt-2");
        payload.put("eventType", "PRODUCT_DELETED");
        payload.put("productId", 2002L);

        when(productSearchService.isEventProcessed("evt-2")).thenReturn(false);

        consumer.accept(MessageBuilder.withPayload(payload).build());

        verify(productSearchService).deleteProduct(2002L);
        verify(productSearchService).markEventProcessed("evt-2");
        verify(productSearchService, never()).upsertProduct(org.mockito.ArgumentMatchers.any(ProductDocument.class));
    }
}

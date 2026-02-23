package com.cloud.product.messaging;

import com.cloud.product.module.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchSyncProducerTest {

    @Mock
    private StreamBridge streamBridge;

    private ProductSearchSyncProducer producer;

    @BeforeEach
    void setUp() {
        producer = new ProductSearchSyncProducer(streamBridge);
        ReflectionTestUtils.setField(producer, "searchSyncEnabled", true);
    }

    @Test
    void shouldSendProductCreatedEventWhenEnabled() {
        when(streamBridge.send(eq("search-producer-out-0"), any())).thenReturn(true);

        Product product = new Product();
        product.setId(101L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("4999.00"));
        product.setStock(20);
        product.setStatus(1);

        boolean sent = producer.sendProductCreated(product);

        assertThat(sent).isTrue();
        verify(streamBridge).send(eq("search-producer-out-0"), any());
    }

    @Test
    void shouldReturnTrueWithoutSendingWhenSyncDisabled() {
        ReflectionTestUtils.setField(producer, "searchSyncEnabled", false);

        boolean sent = producer.sendProductDeleted(102L);

        assertThat(sent).isTrue();
        verifyNoInteractions(streamBridge);
    }
}

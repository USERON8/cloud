package com.cloud.search.messaging;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.ProductSyncEvent;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.support.ProductDocumentAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "product-sync",
    consumerGroup = "search-product-sync-group",
    selectorExpression = "PRODUCT_UPSERT||PRODUCT_DELETE")
public class ProductSyncConsumer extends AbstractMqConsumer<ProductSyncEvent> {

  private static final String NS_PRODUCT_SYNC = "search:product:sync";

  private final ProductDocumentRepository productDocumentRepository;
  private final ObjectMapper objectMapper;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @Override
  protected void doConsume(ProductSyncEvent event, MessageExt msgExt) {
    if (event == null || event.getSpuId() == null) {
      return;
    }
    Long spuId = event.getSpuId();
    if ("PRODUCT_DELETE".equalsIgnoreCase(event.getEventType())) {
      productDocumentRepository.deleteById(String.valueOf(spuId));
      return;
    }
    SpuDetailVO spu =
        invokeProductService("get spu by id", () -> productDubboApi.getSpuById(spuId));
    if (spu == null) {
      productDocumentRepository.deleteById(String.valueOf(spuId));
      return;
    }
    ProductDocument document = ProductDocumentAssembler.toDocument(spu);
    if (document != null) {
      productDocumentRepository.save(document);
    }
  }

  @Override
  protected ProductSyncEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, ProductSyncEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize ProductSyncEvent", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, ProductSyncEvent payload) {
    return NS_PRODUCT_SYNC;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, ProductSyncEvent payload, MessageExt msgExt) {
    return resolveEventId(payload);
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

  private <T> T invokeProductService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "product-service unavailable when " + action, ex);
    }
  }
}

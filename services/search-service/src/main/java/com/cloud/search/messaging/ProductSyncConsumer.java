package com.cloud.search.messaging;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.ProductSyncEvent;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ProductDocumentBuildService;
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
public class ProductSyncConsumer extends AbstractJsonMqConsumer<ProductSyncEvent> {

  private static final String NS_PRODUCT_SYNC = "search:product:sync";

  private final ProductDocumentBuildService productDocumentBuildService;
  private final ProductDocumentRepository productDocumentRepository;

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
    ProductDocument document = productDocumentBuildService.build(spu);
    if (document != null) {
      productDocumentRepository.save(document);
    }
  }

  @Override
  protected Class<ProductSyncEvent> payloadClass() {
    return ProductSyncEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "ProductSyncEvent";
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
    return resolveEventId("PRODUCT_SYNC", event.getEventId(), event.getSpuId());
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

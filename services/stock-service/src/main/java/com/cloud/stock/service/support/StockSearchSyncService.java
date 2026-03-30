package com.cloud.stock.service.support;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.messaging.event.ProductSyncEvent;
import com.cloud.stock.messaging.StockMessageProducer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSearchSyncService {

  private final StockMessageProducer stockMessageProducer;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  public void syncProductsBySkuIds(Collection<Long> skuIds) {
    if (skuIds == null || skuIds.isEmpty()) {
      return;
    }
    List<Long> safeSkuIds = skuIds.stream().filter(id -> id != null).distinct().toList();
    if (safeSkuIds.isEmpty()) {
      return;
    }
    Map<Long, Long> spuIdBySkuId = productDubboApi.mapSpuIdsBySkuIds(safeSkuIds);
    if (spuIdBySkuId == null || spuIdBySkuId.isEmpty()) {
      return;
    }
    Set<Long> spuIds = new LinkedHashSet<>(spuIdBySkuId.values());
    for (Long spuId : spuIds) {
      if (spuId == null) {
        continue;
      }
      boolean sent =
          stockMessageProducer.sendProductSyncEvent(
              ProductSyncEvent.builder().spuId(spuId).eventType("PRODUCT_UPSERT").build());
      if (!sent) {
        log.warn("Skip search sync after stock change: spuId={}", spuId);
      }
    }
  }
}

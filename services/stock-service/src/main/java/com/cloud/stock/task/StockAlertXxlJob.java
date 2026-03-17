package com.cloud.stock.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.messaging.event.StockAlertEvent;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.service.StockLedgerQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertXxlJob {

  private static final int DEFAULT_PAGE_SIZE = 200;
  private static final String WS_CHANNEL_PREFIX = "ws:message:";

  private final StockLedgerQueryService stockLedgerQueryService;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final StockMessageProducer stockMessageProducer;

  @Value("${stock.alert.enabled:true}")
  private boolean alertEnabled;

  @Value("${stock.alert.limit:200}")
  private int alertLimit;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @XxlJob("stockAlertJob")
  @DistributedLock(
      key = "'xxl:stock:alert'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void stockAlert() {
    if (!alertEnabled) {
      XxlJobHelper.log("stockAlertJob skipped, alert disabled");
      return;
    }
    int limit = alertLimit <= 0 ? 200 : alertLimit;
    List<StockLedger> ledgers = loadLowStockLedgers(limit);
    if (ledgers.isEmpty()) {
      XxlJobHelper.log("stockAlertJob finished, empty ledgers");
      return;
    }

    Map<Long, Long> skuToSpu = resolveSkuSpuMapping(ledgers);
    Map<Long, Long> spuToMerchant = resolveSpuMerchantMapping(skuToSpu.values());

    int notified = 0;
    for (StockLedger ledger : ledgers) {
      if (ledger == null || ledger.getSkuId() == null) {
        continue;
      }
      Long spuId = skuToSpu.get(ledger.getSkuId());
      Long merchantId = spuId == null ? null : spuToMerchant.get(spuId);
      if (merchantId == null) {
        continue;
      }
      boolean sentWs = sendAlertMessage(merchantId, ledger);
      boolean sentMq = sendAlertEvent(merchantId, ledger);
      if (sentWs || sentMq) {
        notified++;
      }
    }

    String message = "stockAlertJob finished, notified=" + notified;
    XxlJobHelper.log(message);
    log.info(message);
  }

  private List<StockLedger> loadLowStockLedgers(int limit) {
    int pageSize = Math.min(DEFAULT_PAGE_SIZE, limit);
    int pageIndex = 1;
    List<StockLedger> results = new ArrayList<>();
    while (results.size() < limit) {
      Page<StockLedger> page = stockLedgerQueryService.pageLowStockLedgers(pageIndex, pageSize);
      List<StockLedger> records = page.getRecords();
      if (records == null || records.isEmpty()) {
        break;
      }
      for (StockLedger ledger : records) {
        results.add(ledger);
        if (results.size() >= limit) {
          break;
        }
      }
      if (records.size() < pageSize) {
        break;
      }
      pageIndex++;
    }
    return results;
  }

  private Map<Long, Long> resolveSkuSpuMapping(List<StockLedger> ledgers) {
    Set<Long> skuIds = new HashSet<>();
    for (StockLedger ledger : ledgers) {
      if (ledger != null && ledger.getSkuId() != null) {
        skuIds.add(ledger.getSkuId());
      }
    }
    if (skuIds.isEmpty()) {
      return Map.of();
    }
    List<SkuDetailVO> skuDetails =
        invokeProductService(
            "list sku by ids", () -> productDubboApi.listSkuByIds(new ArrayList<>(skuIds)));
    if (skuDetails == null || skuDetails.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> mapping = new HashMap<>();
    for (SkuDetailVO sku : skuDetails) {
      if (sku.getSkuId() != null && sku.getSpuId() != null) {
        mapping.put(sku.getSkuId(), sku.getSpuId());
      }
    }
    return mapping;
  }

  private Map<Long, Long> resolveSpuMerchantMapping(Iterable<Long> spuIds) {
    Map<Long, Long> mapping = new HashMap<>();
    if (spuIds == null) {
      return mapping;
    }
    for (Long spuId : spuIds) {
      if (spuId == null || mapping.containsKey(spuId)) {
        continue;
      }
      SpuDetailVO spu =
          invokeProductService("get spu by id", () -> productDubboApi.getSpuById(spuId));
      if (spu != null && spu.getMerchantId() != null) {
        mapping.put(spuId, spu.getMerchantId());
      }
    }
    return mapping;
  }

  private boolean sendAlertMessage(Long merchantId, StockLedger ledger) {
    try {
      Map<String, Object> message =
          Map.of(
              "type", "STOCK_ALERT",
              "merchantId", String.valueOf(merchantId),
              "data",
                  Map.of(
                      "skuId", ledger.getSkuId(),
                      "salableQty", ledger.getSalableQty(),
                      "alertThreshold", ledger.getAlertThreshold()),
              "timestamp", Instant.now().toEpochMilli());
      String payload = objectMapper.writeValueAsString(message);
      stringRedisTemplate.convertAndSend(WS_CHANNEL_PREFIX + merchantId, payload);
      return true;
    } catch (Exception ex) {
      log.warn(
          "Send stock alert failed: merchantId={}, skuId={}", merchantId, ledger.getSkuId(), ex);
      return false;
    }
  }

  private boolean sendAlertEvent(Long merchantId, StockLedger ledger) {
    StockAlertEvent event =
        StockAlertEvent.builder()
            .merchantId(merchantId)
            .skuId(ledger.getSkuId())
            .salableQty(ledger.getSalableQty())
            .alertThreshold(ledger.getAlertThreshold())
            .build();
    return stockMessageProducer.sendStockAlertEvent(event);
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

package com.cloud.search.service;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.support.ProductDocumentAssembler;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductDocumentBuildService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private OrderDubboApi orderDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockDubboApi stockDubboApi;

  public ProductDocument build(SpuDetailVO spu) {
    if (spu == null) {
      return null;
    }
    return buildAll(List.of(spu)).stream().findFirst().orElse(null);
  }

  public List<ProductDocument> buildAll(List<SpuDetailVO> spus) {
    if (spus == null || spus.isEmpty()) {
      return List.of();
    }

    List<SpuDetailVO> safeSpus = spus.stream().filter(Objects::nonNull).toList();
    if (safeSpus.isEmpty()) {
      return List.of();
    }

    Map<Long, Integer> salesCountByProductId = loadSalesCountByProductId(safeSpus);
    Map<Long, Integer> stockQuantityByProductId = loadStockQuantityByProductId(safeSpus);

    return safeSpus.stream()
        .map(
            spu ->
                ProductDocumentAssembler.toDocument(
                    spu,
                    stockQuantityByProductId.get(spu.getSpuId()),
                    salesCountByProductId.get(spu.getSpuId())))
        .filter(Objects::nonNull)
        .toList();
  }

  private Map<Long, Integer> loadSalesCountByProductId(List<SpuDetailVO> spus) {
    List<Long> productIds =
        spus.stream()
            .map(SpuDetailVO::getSpuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (productIds.isEmpty()) {
      return Map.of();
    }

    List<ProductSellStatDTO> stats =
        invokeOrderService(
            "stat sell count by product ids",
            () -> orderDubboApi.statSellCountByProductIds(productIds));
    if (stats == null || stats.isEmpty()) {
      return Map.of();
    }

    return stats.stream()
        .filter(stat -> stat != null && stat.getProductId() != null)
        .collect(
            Collectors.toMap(
                ProductSellStatDTO::getProductId,
                stat -> toSafeInteger(stat.getSellCount()),
                Integer::max));
  }

  private Map<Long, Integer> loadStockQuantityByProductId(List<SpuDetailVO> spus) {
    Map<Long, Long> productIdBySkuId =
        spus.stream()
            .filter(spu -> spu.getSpuId() != null && spu.getSkus() != null)
            .flatMap(
                spu ->
                    spu.getSkus().stream()
                        .filter(sku -> sku != null && sku.getSkuId() != null)
                        .map(sku -> Map.entry(sku.getSkuId(), spu.getSpuId())))
            .collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    if (productIdBySkuId.isEmpty()) {
      return Map.of();
    }

    List<Long> skuIds = productIdBySkuId.keySet().stream().toList();
    List<StockLedgerVO> ledgers =
        invokeStockService(
            "list ledgers by sku ids", () -> stockDubboApi.listLedgersBySkuIds(skuIds));
    if (ledgers == null || ledgers.isEmpty()) {
      return Map.of();
    }

    Map<Long, Long> quantityByProductId = new HashMap<>();
    for (StockLedgerVO ledger : ledgers) {
      if (ledger == null || ledger.getSkuId() == null) {
        continue;
      }
      Long productId = productIdBySkuId.get(ledger.getSkuId());
      if (productId == null) {
        continue;
      }
      quantityByProductId.merge(productId, toSafeLong(ledger.getSalableQty()), Long::sum);
    }

    return quantityByProductId.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> toSafeInteger(entry.getValue())));
  }

  private Integer toSafeInteger(Long value) {
    if (value == null || value <= 0) {
      return 0;
    }
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
  }

  private Long toSafeLong(Integer value) {
    if (value == null || value <= 0) {
      return 0L;
    }
    return value.longValue();
  }

  private <T> T invokeOrderService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "order-service unavailable when " + action, ex);
    }
  }

  private <T> T invokeStockService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock-service unavailable when " + action, ex);
    }
  }
}

package com.cloud.search.messaging;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.CanalFlatMessage;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.ProductDocumentBuildService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "${search.canal.topic:product-canal}",
    consumerGroup = "${search.canal.consumer-group:search-canal-sync-group}")
public class CanalSearchSyncConsumer extends AbstractJsonMqConsumer<CanalFlatMessage> {

  private static final String NS_CANAL_SYNC = "search:canal:sync";
  private static final String TABLE_SPU = "spu";
  private static final String TABLE_SKU = "sku";
  private static final String TABLE_CATEGORY = "category";
  private static final String TABLE_SHOP = "merchant_shop";
  private static final String TABLE_STOCK_SEGMENT = "stock_segment";
  private static final String TYPE_DELETE = "DELETE";
  private static final DateTimeFormatter MYSQL_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final RemoteCallSupport remoteCallSupport;
  private final ProductDocumentBuildService productDocumentBuildService;
  private final ProductDocumentRepository productDocumentRepository;
  private final ShopDocumentRepository shopDocumentRepository;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @Override
  protected void doConsume(CanalFlatMessage event, MessageExt msgExt) {
    if (event == null || Boolean.TRUE.equals(event.getIsDdl())) {
      return;
    }
    String table = normalizeTable(event.getTable());
    List<Map<String, Object>> rows = event.getData();
    if (table == null || rows == null || rows.isEmpty()) {
      return;
    }
    for (Map<String, Object> row : rows) {
      syncRow(event, table, row);
    }
  }

  @Override
  protected Class<CanalFlatMessage> payloadClass() {
    return CanalFlatMessage.class;
  }

  @Override
  protected String payloadDescription() {
    return "CanalFlatMessage";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, CanalFlatMessage payload) {
    return NS_CANAL_SYNC;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, CanalFlatMessage payload, MessageExt msgExt) {
    if (payload == null) {
      return msgId == null ? "" : msgId;
    }
    return resolveEventId(
        "CANAL",
        null,
        payload.getId(),
        payload.getDatabase(),
        payload.getTable(),
        payload.getType(),
        payload.getEs());
  }

  private void syncRow(CanalFlatMessage event, String table, Map<String, Object> row) {
    switch (table) {
      case TABLE_SPU -> syncSpuRow(event, row);
      case TABLE_SKU -> syncSkuRow(row);
      case TABLE_CATEGORY -> syncCategoryRow(row);
      case TABLE_SHOP -> syncShopRow(event, row);
      case TABLE_STOCK_SEGMENT -> syncStockSegmentRow(row);
      default -> log.debug("Ignore canal table for search sync: table={}", table);
    }
  }

  private void syncSpuRow(CanalFlatMessage event, Map<String, Object> row) {
    Long spuId = readCanalLong(row, "id");
    if (spuId == null) {
      return;
    }
    if (isDelete(event) || isDeleted(row)) {
      productDocumentRepository.deleteById(String.valueOf(spuId));
      return;
    }
    refreshProductBySpuId(spuId);
  }

  private void syncSkuRow(Map<String, Object> row) {
    Long spuId = readCanalLong(row, "spu_id");
    if (spuId != null) {
      refreshProductBySpuId(spuId);
      return;
    }
    Long skuId = readCanalLong(row, "id");
    if (skuId != null) {
      refreshProductsBySkuIds(List.of(skuId));
    }
  }

  private void syncStockSegmentRow(Map<String, Object> row) {
    Long skuId = readCanalLong(row, "sku_id");
    if (skuId != null) {
      refreshProductsBySkuIds(List.of(skuId));
    }
  }

  private void syncCategoryRow(Map<String, Object> row) {
    Long categoryId = readCanalLong(row, "id");
    if (categoryId == null) {
      return;
    }
    List<SpuDetailVO> spus =
        remoteCallSupport.query(
            "product-service.listSpuByCategory",
            () -> productDubboApi.listSpuByCategory(categoryId, null));
    saveProductDocuments(spus);
  }

  private void syncShopRow(CanalFlatMessage event, Map<String, Object> row) {
    Long shopId = readCanalLong(row, "id");
    Long merchantId = readCanalLong(row, "merchant_id");
    if (shopId != null) {
      if (isDelete(event) || isDeleted(row)) {
        shopDocumentRepository.deleteById(String.valueOf(shopId));
      } else {
        shopDocumentRepository.save(toShopDocument(row, shopId, merchantId));
      }
    }
    if (merchantId != null) {
      refreshProductsByMerchantId(merchantId);
    }
  }

  private void refreshProductsBySkuIds(List<Long> skuIds) {
    Map<Long, Long> spuIdBySkuId =
        remoteCallSupport.query(
            "product-service.mapSpuIdsBySkuIds", () -> productDubboApi.mapSpuIdsBySkuIds(skuIds));
    if (spuIdBySkuId == null || spuIdBySkuId.isEmpty()) {
      return;
    }
    spuIdBySkuId.values().stream()
        .filter(id -> id != null)
        .distinct()
        .forEach(this::refreshProductBySpuId);
  }

  private void refreshProductsByMerchantId(Long merchantId) {
    List<SpuDetailVO> spus =
        remoteCallSupport.query(
            "product-service.listSpuByMerchantId",
            () -> productDubboApi.listSpuByMerchantId(merchantId, null));
    saveProductDocuments(spus);
  }

  private void refreshProductBySpuId(Long spuId) {
    SpuDetailVO spu =
        remoteCallSupport.query(
            "product-service.getSpuById", () -> productDubboApi.getSpuById(spuId));
    if (spu == null) {
      productDocumentRepository.deleteById(String.valueOf(spuId));
      return;
    }
    ProductDocument document = productDocumentBuildService.build(spu);
    if (document != null) {
      productDocumentRepository.save(document);
    }
  }

  private void saveProductDocuments(List<SpuDetailVO> spus) {
    List<ProductDocument> documents = productDocumentBuildService.buildAll(spus);
    if (documents == null || documents.isEmpty()) {
      return;
    }
    productDocumentRepository.saveAll(documents);
  }

  private ShopDocument toShopDocument(Map<String, Object> row, Long shopId, Long merchantId) {
    return ShopDocument.builder()
        .id(String.valueOf(shopId))
        .shopId(shopId)
        .merchantId(merchantId)
        .shopName(readCanalString(row, "shop_name"))
        .shopNameKeyword(readCanalString(row, "shop_name"))
        .avatarUrl(readCanalString(row, "avatar_url"))
        .description(readCanalString(row, "description"))
        .contactPhone(readCanalString(row, "contact_phone"))
        .address(readCanalString(row, "address"))
        .status(readInteger(row, "status"))
        .createdAt(readDateTime(row, "created_at"))
        .updatedAt(readDateTime(row, "updated_at"))
        .searchWeight(0D)
        .hotScore(0D)
        .recommended(false)
        .build();
  }

  private String normalizeTable(String table) {
    if (table == null || table.isBlank()) {
      return null;
    }
    return table.trim().toLowerCase();
  }

  private boolean isDelete(CanalFlatMessage event) {
    return event != null && TYPE_DELETE.equalsIgnoreCase(event.getType());
  }

  private boolean isDeleted(Map<String, Object> row) {
    Integer deleted = readInteger(row, "deleted");
    return deleted != null && deleted == 1;
  }

  private String readCanalString(Map<String, Object> row, String key) {
    Object value = row == null ? null : row.get(key);
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private Long readCanalLong(Map<String, Object> row, String key) {
    String value = readCanalString(row, key);
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      log.debug("Ignore non-long canal field: key={}, value={}", key, value, ex);
      return null;
    }
  }

  private Integer readInteger(Map<String, Object> row, String key) {
    String value = readCanalString(row, key);
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      log.debug("Ignore non-integer canal field: key={}, value={}", key, value, ex);
      return null;
    }
  }

  private LocalDateTime readDateTime(Map<String, Object> row, String key) {
    String value = readCanalString(row, key);
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(value, MYSQL_DATE_TIME);
    } catch (Exception ignored) {
      try {
        return LocalDateTime.parse(value);
      } catch (Exception ex) {
        log.debug("Ignore non-datetime canal field: key={}, value={}", key, value, ex);
        return null;
      }
    }
  }
}

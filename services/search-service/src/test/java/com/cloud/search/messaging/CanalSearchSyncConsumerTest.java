package com.cloud.search.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.messaging.event.CanalFlatMessage;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.ProductDocumentBuildService;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CanalSearchSyncConsumerTest {

  @Mock private RemoteCallSupport remoteCallSupport;
  @Mock private ProductDocumentBuildService productDocumentBuildService;
  @Mock private ProductDocumentRepository productDocumentRepository;
  @Mock private ShopDocumentRepository shopDocumentRepository;

  @Test
  void spuUpdateRefreshesProductDocument() {
    CanalSearchSyncConsumer consumer = newConsumer();
    SpuDetailVO spu = new SpuDetailVO();
    spu.setSpuId(1001L);
    ProductDocument document = ProductDocument.builder().id("1001").productId(1001L).build();
    when(remoteCallSupport.query(eq("product-service.getSpuById"), any(Supplier.class)))
        .thenReturn(spu);
    when(productDocumentBuildService.build(spu)).thenReturn(document);

    consumer.doConsume(canal("spu", "UPDATE", Map.of("id", "1001", "deleted", "0")), null);

    verify(productDocumentRepository).save(document);
  }

  @Test
  void spuDeleteRemovesProductDocument() {
    CanalSearchSyncConsumer consumer = newConsumer();

    consumer.doConsume(canal("spu", "DELETE", Map.of("id", "1001")), null);

    verify(productDocumentRepository).deleteById("1001");
    verify(remoteCallSupport, never()).query(eq("product-service.getSpuById"), any());
  }

  @Test
  void shopUpdateSavesShopAndRefreshesMerchantProducts() {
    CanalSearchSyncConsumer consumer = newConsumer();
    SpuDetailVO spu = new SpuDetailVO();
    spu.setSpuId(1001L);
    ProductDocument document = ProductDocument.builder().id("1001").productId(1001L).build();
    when(remoteCallSupport.query(eq("product-service.listSpuByMerchantId"), any(Supplier.class)))
        .thenReturn(List.of(spu));
    when(productDocumentBuildService.buildAll(List.of(spu))).thenReturn(List.of(document));

    consumer.doConsume(
        canal(
            "merchant_shop",
            "UPDATE",
            Map.of("id", "3001", "merchant_id", "2001", "shop_name", "Cloud Store", "status", "1")),
        null);

    ArgumentCaptor<ShopDocument> shopCaptor = ArgumentCaptor.forClass(ShopDocument.class);
    verify(shopDocumentRepository).save(shopCaptor.capture());
    verify(productDocumentRepository).saveAll(List.of(document));
    org.assertj.core.api.Assertions.assertThat(shopCaptor.getValue().getShopId()).isEqualTo(3001L);
    org.assertj.core.api.Assertions.assertThat(shopCaptor.getValue().getMerchantId())
        .isEqualTo(2001L);
    org.assertj.core.api.Assertions.assertThat(shopCaptor.getValue().getShopName())
        .isEqualTo("Cloud Store");
  }

  @Test
  void stockSegmentUpdateRefreshesProductBySkuMapping() {
    CanalSearchSyncConsumer consumer = newConsumer();
    SpuDetailVO spu = new SpuDetailVO();
    spu.setSpuId(1001L);
    ProductDocument document = ProductDocument.builder().id("1001").productId(1001L).build();
    when(remoteCallSupport.query(eq("product-service.mapSpuIdsBySkuIds"), any(Supplier.class)))
        .thenReturn(Map.of(5001L, 1001L));
    when(remoteCallSupport.query(eq("product-service.getSpuById"), any(Supplier.class)))
        .thenReturn(spu);
    when(productDocumentBuildService.build(spu)).thenReturn(document);

    consumer.doConsume(canal("stock_segment", "UPDATE", Map.of("sku_id", "5001")), null);

    verify(productDocumentRepository).save(document);
  }

  private CanalSearchSyncConsumer newConsumer() {
    return new CanalSearchSyncConsumer(
        remoteCallSupport,
        productDocumentBuildService,
        productDocumentRepository,
        shopDocumentRepository);
  }

  private CanalFlatMessage canal(String table, String type, Map<String, Object> row) {
    CanalFlatMessage message = new CanalFlatMessage();
    message.setId(1L);
    message.setDatabase("product_db");
    message.setTable(table);
    message.setType(type);
    message.setIsDdl(false);
    message.setData(List.of(row));
    return message;
  }
}

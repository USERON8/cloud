package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.search.document.ProductDocument;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDocumentBuildServiceTest {

  @Mock private OrderDubboApi orderDubboApi;

  @Mock private StockDubboApi stockDubboApi;

  @InjectMocks private ProductDocumentBuildService productDocumentBuildService;

  @Test
  void buildAll_shouldEnrichDocumentsWithStockAndSales() {
    SkuDetailVO firstSku = new SkuDetailVO();
    firstSku.setSkuId(501L);
    firstSku.setSkuCode("SKU-501");

    SkuDetailVO secondSku = new SkuDetailVO();
    secondSku.setSkuId(502L);
    secondSku.setSkuCode("SKU-502");

    SpuDetailVO spu = new SpuDetailVO();
    spu.setSpuId(1001L);
    spu.setSpuName("Cloud Phone");
    spu.setCreatedAt(LocalDateTime.now().minusDays(3));
    spu.setSkus(List.of(firstSku, secondSku));

    ProductSellStatDTO stat = new ProductSellStatDTO();
    stat.setProductId(1001L);
    stat.setSellCount(19L);

    StockLedgerVO firstLedger = new StockLedgerVO();
    firstLedger.setSkuId(501L);
    firstLedger.setSalableQty(7);
    StockLedgerVO secondLedger = new StockLedgerVO();
    secondLedger.setSkuId(502L);
    secondLedger.setSalableQty(5);

    when(orderDubboApi.statSellCountByProductIds(List.of(1001L))).thenReturn(List.of(stat));
    when(stockDubboApi.listLedgersBySkuIds(List.of(501L, 502L)))
        .thenReturn(List.of(firstLedger, secondLedger));

    List<ProductDocument> result = productDocumentBuildService.buildAll(List.of(spu));

    assertThat(result).hasSize(1);
    ProductDocument document = result.get(0);
    assertThat(document.getProductId()).isEqualTo(1001L);
    assertThat(document.getSalesCount()).isEqualTo(19);
    assertThat(document.getStockQuantity()).isEqualTo(12);
  }
}

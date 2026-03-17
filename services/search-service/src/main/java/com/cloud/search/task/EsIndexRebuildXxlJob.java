package com.cloud.search.task;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.CategorySearchService;
import com.cloud.search.service.ShopSearchService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexRebuildXxlJob {

  private static final int DEFAULT_PAGE_SIZE = 200;

  private final ProductDocumentRepository productDocumentRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final CategorySearchService categorySearchService;
  private final ShopSearchService shopSearchService;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @XxlJob("esIndexRebuildJob")
  @DistributedLock(
      key = "'xxl:search:es-index-rebuild'",
      waitTime = 1,
      leaseTime = 1800,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void rebuildIndex() {
    try {
      categorySearchService.rebuildCategoryIndex();
      shopSearchService.rebuildShopIndex();
      int total = rebuildProductIndex();
      String message = "esIndexRebuildJob finished, total=" + total;
      XxlJobHelper.log(message);
      log.info(message);
    } catch (Exception ex) {
      log.error("esIndexRebuildJob failed", ex);
      XxlJobHelper.handleFail(ex.getMessage());
    }
  }

  private int rebuildProductIndex() {
    if (elasticsearchOperations.indexOps(ProductDocument.class).exists()) {
      elasticsearchOperations.indexOps(ProductDocument.class).delete();
    }
    elasticsearchOperations.indexOps(ProductDocument.class).create();
    elasticsearchOperations.indexOps(ProductDocument.class).putMapping();

    int total = 0;
    int page = 1;
    int size = DEFAULT_PAGE_SIZE;
    while (true) {
      List<SpuDetailVO> spus =
          invokeProductService(
              "list spu by page", () -> productDubboApi.listSpuByPage(page, size, 1));
      if (spus == null || spus.isEmpty()) {
        break;
      }
      List<ProductDocument> docs =
          spus.stream().map(this::toDocument).filter(doc -> doc != null).toList();
      if (!docs.isEmpty()) {
        productDocumentRepository.saveAll(docs);
        total += docs.size();
      }
      if (spus.size() < size) {
        break;
      }
      page++;
    }
    return total;
  }

  private ProductDocument toDocument(SpuDetailVO spu) {
    if (spu == null) {
      return null;
    }
    List<SkuDetailVO> skus = spu.getSkus();
    Optional<SkuDetailVO> minPriceSku =
        skus == null
            ? Optional.empty()
            : skus.stream()
                .filter(sku -> sku.getSalePrice() != null)
                .min(Comparator.comparing(SkuDetailVO::getSalePrice));

    BigDecimal price = minPriceSku.map(SkuDetailVO::getSalePrice).orElse(null);
    String skuCode = minPriceSku.map(SkuDetailVO::getSkuCode).orElse(null);
    String imageUrl = minPriceSku.map(SkuDetailVO::getImageUrl).orElse(spu.getMainImage());

    return ProductDocument.builder()
        .id(String.valueOf(spu.getSpuId()))
        .productId(spu.getSpuId())
        .productName(spu.getSpuName())
        .productNameKeyword(spu.getSpuName())
        .price(price)
        .categoryId(spu.getCategoryId())
        .brandId(spu.getBrandId())
        .merchantId(spu.getMerchantId())
        .status(spu.getStatus())
        .description(spu.getDescription())
        .imageUrl(imageUrl)
        .sku(skuCode)
        .createdAt(spu.getCreatedAt())
        .updatedAt(spu.getUpdatedAt())
        .build();
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

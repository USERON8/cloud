package com.cloud.product.rpc;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService(interfaceClass = ProductDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class ProductCatalogDubboService implements ProductDubboApi {

    private final ProductCatalogService productCatalogService;

    @Override
    public Long createSpu(SpuCreateRequestDTO request) {
        return productCatalogService.createSpu(request);
    }

    @Override
    public Boolean updateSpu(Long spuId, SpuCreateRequestDTO request) {
        return productCatalogService.updateSpu(spuId, request);
    }

    @Override
    public SpuDetailVO getSpuById(Long spuId) {
        return productCatalogService.getSpuById(spuId);
    }

    @Override
    public List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status) {
        return productCatalogService.listSpuByCategory(categoryId, status);
    }

    @Override
    public List<SpuDetailVO> listSpuByPage(Integer page, Integer size, Integer status) {
        return productCatalogService.listSpuByPage(page, size, status);
    }

    @Override
    public List<SkuDetailVO> listSkuByIds(List<Long> skuIds) {
        return productCatalogService.listSkuByIds(skuIds);
    }

    @Override
    public Boolean updateSpuStatus(Long spuId, Integer status) {
        return productCatalogService.updateSpuStatus(spuId, status);
    }
}


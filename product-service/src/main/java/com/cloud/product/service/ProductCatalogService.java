package com.cloud.product.service;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;

import java.util.List;

public interface ProductCatalogService {

    Long createSpu(SpuCreateRequestDTO request);

    Boolean updateSpu(Long spuId, SpuCreateRequestDTO request);

    SpuDetailVO getSpuById(Long spuId);

    List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status);

    List<SkuDetailVO> listSkuByIds(List<Long> skuIds);

    Boolean updateSpuStatus(Long spuId, Integer status);
}

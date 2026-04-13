package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import java.util.List;
import java.util.Map;

public interface ProductDubboApi {

  Long createSpu(SpuCreateRequestDTO request);

  Boolean updateSpu(Long spuId, SpuCreateRequestDTO request);

  SpuDetailVO getSpuById(Long spuId);

  List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status);

  List<SpuDetailVO> listSpuByPage(Integer page, Integer size, Integer status);

  List<SkuDetailVO> listSkuByIds(List<Long> skuIds);

  List<ProductSearchItemDTO> searchProducts(String name, Integer size);

  List<String> suggestProducts(String keyword, Integer size);

  Map<Long, Long> mapSpuIdsBySkuIds(List<Long> skuIds);

  Boolean updateSpuStatus(Long spuId, Integer status);
}

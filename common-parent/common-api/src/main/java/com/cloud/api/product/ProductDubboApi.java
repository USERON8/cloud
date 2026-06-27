package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import java.util.List;
import java.util.Map;

/**
 * 商品服务内部调用契约。
 *
 * <p>订单、搜索、库存预校验和治理任务通过该接口读取 SPU/SKU 资料，商品服务仍是商品主数据的唯一写入口。
 */
public interface ProductDubboApi {

  /** 创建 SPU 及其关联 SKU。 */
  Long createSpu(SpuCreateRequestDTO request);

  /** 更新 SPU 及其关联 SKU。 */
  Boolean updateSpu(Long spuId, SpuCreateRequestDTO request);

  /** 按 SPU ID 查询商品详情。 */
  SpuDetailVO getSpuById(Long spuId);

  /** 按分类查询 SPU 列表。 */
  List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status);

  /** 按商户查询 SPU 列表。 */
  List<SpuDetailVO> listSpuByMerchantId(Long merchantId, Integer status);

  /** 分页查询 SPU 列表。 */
  List<SpuDetailVO> listSpuByPage(Integer page, Integer size, Integer status);

  /** 按 SKU ID 批量查询 SKU 明细。 */
  List<SkuDetailVO> listSkuByIds(List<Long> skuIds);

  /** 查询商品搜索候选项，用于网关降级和搜索建议。 */
  List<ProductSearchItemDTO> searchProducts(String name, Integer size);

  /** 查询商品搜索建议词。 */
  List<String> suggestProducts(String keyword, Integer size);

  /** 将 SKU ID 映射为所属 SPU ID。 */
  Map<Long, Long> mapSpuIdsBySkuIds(List<Long> skuIds);

  /** 更新 SPU 上下架或审核状态。 */
  Boolean updateSpuStatus(Long spuId, Integer status);
}

package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.SkuDTO;
import com.cloud.common.domain.dto.product.SpuDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductDetailConverter {

  @Mapping(target = "id", source = "spuId")
  Spu toEntity(SpuDTO dto);

  @Mapping(target = "id", source = "skuId")
  Sku toEntity(SkuDTO dto);

  @Mapping(target = "spuId", source = "id")
  SpuDetailVO toSpuDetailVO(Spu spu);

  @Mapping(target = "skuId", source = "id")
  SkuDetailVO toSkuDetailVO(Sku sku);

  @Mapping(target = "skus", ignore = true)
  SpuDetailVO copyBase(SpuDetailVO source);
}

package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductItemConverter {

  @Mapping(target = "id", source = "spu.id")
  @Mapping(target = "shopId", source = "spu.merchantId")
  @Mapping(target = "name", source = "spu.spuName")
  @Mapping(target = "price", source = "sku.salePrice")
  @Mapping(target = "status", source = "spu.status")
  @Mapping(target = "imageUrl", source = "sku.imageUrl")
  ProductItemDTO toDTO(Spu spu, Sku sku);

  @Mapping(target = "id", source = "spu.id")
  @Mapping(target = "shopId", source = "spu.merchantId")
  @Mapping(target = "name", source = "spu.spuName")
  @Mapping(target = "price", source = "sku.salePrice")
  @Mapping(target = "status", source = "spu.status")
  @Mapping(target = "imageUrl", source = "sku.imageUrl")
  ProductSearchItemDTO toSearchItemDTO(Spu spu, Sku sku);
}

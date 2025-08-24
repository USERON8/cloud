package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.product.module.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 商品转换器
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ProductConverter {
    ProductConverter INSTANCE = Mappers.getMapper(ProductConverter.class);

    /**
     * 转换商品实体为DTO
     *
     * @param product 商品实体
     * @return 商品DTO
     */
    ProductDTO toDTO(Product product);

    /**
     * 转换商品DTO为实体
     *
     * @param productDTO 商品DTO
     * @return 商品实体
     */
    Product toEntity(ProductDTO productDTO);

    /**
     * 转换商品实体列表为DTO列表
     *
     * @param products 商品实体列表
     * @return 商品DTO列表
     */
    List<ProductDTO> toDTOList(List<Product> products);
}
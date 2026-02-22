package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.product.module.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;








@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ProductConverter {
    ProductConverter INSTANCE = Mappers.getMapper(ProductConverter.class);

    

    





    @Mapping(source = "stock", target = "stockQuantity")
    @Mapping(target = "deleted", ignore = true)
    ProductDTO toDTO(Product product);

    





    @Mapping(source = "stockQuantity", target = "stock")
    @Mapping(target = "deleted", ignore = true)
    Product toEntity(ProductDTO productDTO);

    





    List<ProductDTO> toDTOList(List<Product> products);

    

    





    @Mapping(source = "stock", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductVO toVO(Product product);

    





    List<ProductVO> toVOList(List<Product> products);

    

    





    Product requestDTOToEntity(ProductRequestDTO requestDTO);

    

    





    ProductRequestDTO dtoToRequestDTO(ProductDTO productDTO);

    

    





    @Mapping(source = "stockQuantity", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductVO dtoToVO(ProductDTO productDTO);

    





    List<ProductVO> dtoToVOList(List<ProductDTO> productDTOs);

    

    





    @Mapping(source = "stockQuantity", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductDTO voToDTO(ProductVO productVO);

    





    List<ProductDTO> voListToDTOList(List<ProductVO> productVOs);


    





    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "鏈煡";
        }
        return status == 1 ? "涓婃灦" : "涓嬫灦";
    }

}

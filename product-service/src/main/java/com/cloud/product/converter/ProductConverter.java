package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.ProductVO;
import com.cloud.product.module.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 商品转换器
 * 提供商品实体、DTO、VO之间的转换功能
 *
 * @author what's up
 * @since 1.0.0
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ProductConverter {
    ProductConverter INSTANCE = Mappers.getMapper(ProductConverter.class);

    // ================= Entity <-> DTO 转换 =================

    /**
     * 转换商品实体为DTO
     *
     * @param product 商品实体
     * @return 商品DTO
     */
    @Mapping(source = "stock", target = "stockQuantity")
    @Mapping(target = "deleted", ignore = true)
    ProductDTO toDTO(Product product);

    /**
     * 转换商品DTO为实体
     *
     * @param productDTO 商品DTO
     * @return 商品实体
     */
    @Mapping(source = "stockQuantity", target = "stock")
    @Mapping(target = "deleted", ignore = true)
    Product toEntity(ProductDTO productDTO);

    /**
     * 转换商品实体列表为DTO列表
     *
     * @param products 商品实体列表
     * @return 商品DTO列表
     */
    List<ProductDTO> toDTOList(List<Product> products);

    // ================= Entity <-> VO 转换 =================

    /**
     * 转换商品实体为VO
     *
     * @param product 商品实体
     * @return 商品VO
     */
    @Mapping(source = "stock", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductVO toVO(Product product);

    /**
     * 转换商品实体列表为VO列表
     *
     * @param products 商品实体列表
     * @return 商品VO列表
     */
    List<ProductVO> toVOList(List<Product> products);

    // ================= RequestDTO <-> Entity 转换 =================

    /**
     * 转换商品请求DTO为实体
     *
     * @param requestDTO 商品请求DTO
     * @return 商品实体
     */
    Product requestDTOToEntity(ProductRequestDTO requestDTO);

    // ================= DTO <-> RequestDTO 转换 =================

    /**
     * 转换商品DTO为RequestDTO
     *
     * @param productDTO 商品DTO
     * @return 商品请求DTO
     */
    ProductRequestDTO dtoToRequestDTO(ProductDTO productDTO);

    // ================= DTO <-> VO 转换 =================

    /**
     * 转换商品DTO为VO
     *
     * @param productDTO 商品DTO
     * @return 商品VO
     */
    @Mapping(source = "stockQuantity", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductVO dtoToVO(ProductDTO productDTO);

    /**
     * 转换商品DTO列表为VO列表
     *
     * @param productDTOs 商品DTO列表
     * @return 商品VO列表
     */
    List<ProductVO> dtoToVOList(List<ProductDTO> productDTOs);

    // ================= VO <-> DTO 转换 =================

    /**
     * 转换商品VO为DTO
     *
     * @param productVO 商品VO
     * @return 商品DTO
     */
    @Mapping(source = "stockQuantity", target = "stockQuantity")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    ProductDTO voToDTO(ProductVO productVO);

    /**
     * 转换商品VO列表为DTO列表
     *
     * @param productVOs 商品VO列表
     * @return 商品DTO列表
     */
    List<ProductDTO> voListToDTOList(List<ProductVO> productVOs);


    /**
     * 获取商品状态描述
     *
     * @param status 状态值
     * @return 状态描述
     */
    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "上架" : "下架";
    }

}

package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 分类转换器
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface CategoryConverter {
    CategoryConverter INSTANCE = Mappers.getMapper(CategoryConverter.class);

    /**
     * 转换分类实体为DTO
     *
     * @param category 分类实体
     * @return 分类DTO
     */
    @Mapping(target = "children", ignore = true)
    CategoryDTO toDTO(Category category);

    /**
     * 转换分类DTO为实体
     *
     * @param categoryDTO 分类DTO
     * @return 分类实体
     */
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryDTO categoryDTO);

    /**
     * 转换分类实体列表为DTO列表
     *
     * @param categories 分类实体列表
     * @return 分类DTO列表
     */
    @Mapping(target = "children", ignore = true)
    List<CategoryDTO> toDTOList(List<Category> categories);
}
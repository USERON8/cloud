package com.cloud.product.converter;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;
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
public interface CategoryConverter {
    CategoryConverter INSTANCE = Mappers.getMapper(CategoryConverter.class);

    





    @Mapping(target = "deleted", ignore = true)
    CategoryDTO toDTO(Category category);

    





    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Category toEntity(CategoryDTO categoryDTO);

    





    List<CategoryDTO> toDTOList(List<Category> categories);
}

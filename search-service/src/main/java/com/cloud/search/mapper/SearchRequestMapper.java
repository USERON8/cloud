package com.cloud.search.mapper;

import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 搜索请求映射器
 * 使用MapStruct自动生成DTO转换代码
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SearchRequestMapper {

    /**
     * 将ProductFilterRequest转换为ProductSearchRequest
     * MapStruct会自动生成实现代码
     * <p>
     * 注意：ProductSearchRequest中有部分字段在源对象中不存在，这些字段会保持默认值（null或false）
     *
     * @param filterRequest 筛选请求
     * @return 搜索请求
     */
    @Mapping(target = "shopName", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stockStatus", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "minRating", ignore = true)
    @Mapping(target = "highlight", ignore = true)
    @Mapping(target = "includeAggregations", ignore = true)
    ProductSearchRequest toSearchRequest(ProductFilterRequest filterRequest);
}

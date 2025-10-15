package com.cloud.search.mapper;

import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import org.mapstruct.Mapper;
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
     *
     * @param filterRequest 筛选请求
     * @return 搜索请求
     */
    ProductSearchRequest toSearchRequest(ProductFilterRequest filterRequest);
}

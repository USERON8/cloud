package com.cloud.search.mapper;

import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;









@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SearchRequestMapper {

    








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

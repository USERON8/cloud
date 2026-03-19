package com.cloud.order.converter;

import com.cloud.order.dto.AfterSaleDTO;
import com.cloud.order.entity.AfterSale;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.ERROR)
public interface AfterSaleDtoConverter {

  AfterSale toEntity(AfterSaleDTO dto);

  AfterSaleDTO toDto(AfterSale entity);
}

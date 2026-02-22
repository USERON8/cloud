package com.cloud.stock.converter;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.stock.module.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;






@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface StockConverter {

    StockConverter INSTANCE = Mappers.getMapper(StockConverter.class);

    





    StockDTO toDTO(Stock stock);

    





    Stock toEntity(StockDTO stockDTO);


    





    StockVO toVO(Stock stock);

    





    List<StockVO> toVOList(List<Stock> stocks);

    





    List<StockDTO> toDTOList(List<Stock> stocks);
}

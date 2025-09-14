package com.cloud.stock.converter;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.module.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库存转换器
 *
 * @author what's up
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface StockConverter {

    StockConverter INSTANCE = Mappers.getMapper(StockConverter.class);

    /**
     * 转换库存实体为DTO
     *
     * @param stock 库存实体
     * @return 库存DTO
     */
    StockDTO toDTO(Stock stock);

    /**
     * 转换库存DTO为实体
     *
     * @param stockDTO 库存DTO
     * @return 库存实体
     */
    Stock toEntity(StockDTO stockDTO);


    /**
     * 转换库存实体为VO
     *
     * @param stock 库存实体
     * @return 库存VO
     */
    StockVO toVO(Stock stock);

    /**
     * 转换库存实体列表为VO列表
     *
     * @param stocks 库存实体列表
     * @return 库存VO列表
     */
    List<StockVO> toVOList(List<Stock> stocks);

    /**
     * 转换库存实体列表为DTO列表
     *
     * @param stocks 库存实体列表
     * @return 库存DTO列表
     */
    List<StockDTO> toDTOList(List<Stock> stocks);
}

package com.cloud.stock.converter;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.module.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库存实体转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface StockConverter {
    StockConverter INSTANCE = Mappers.getMapper(StockConverter.class);

    /**
     * 实体转VO
     *
     * @param stock 库存实体
     * @return 库存VO
     */
    StockVO toVO(Stock stock);

    /**
     * 实体列表转VO列表
     *
     * @param stockList 库存实体列表
     * @return 库存VO列表
     */
    List<StockVO> toVOList(List<Stock> stockList);

    /**
     * DTO转实体
     *
     * @param stockDTO 库存DTO
     * @return 库存实体
     */
    Stock toEntity(StockDTO stockDTO);

    /**
     * 实体转DTO
     *
     * @param stock 库存实体
     * @return 库存DTO
     */
    StockDTO toDTO(Stock stock);
}
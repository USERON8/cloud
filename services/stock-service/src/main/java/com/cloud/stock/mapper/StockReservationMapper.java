package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockReservationMapper extends BaseMapper<StockReservation> {
}

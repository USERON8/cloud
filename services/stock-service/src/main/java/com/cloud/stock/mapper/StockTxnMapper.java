package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockTxn;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockTxnMapper extends BaseMapper<StockTxn> {
}

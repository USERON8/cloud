package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockIn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库记录Mapper接口
 *
 * @author what's up
 */
@Mapper
public interface StockInMapper extends BaseMapper<StockIn> {

}

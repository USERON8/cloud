package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockFreeze;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author cloud
 * @description 针对表【stock_freeze(库存冻结记录表)】的数据库操作Mapper
 * @createDate 2025-08-24 15:49:56
 * @Entity com.cloud.stock.module.entity.StockFreeze
 */
@Mapper
public interface StockFreezeMapper extends BaseMapper<StockFreeze> {

}
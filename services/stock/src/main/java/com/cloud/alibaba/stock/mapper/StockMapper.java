package com.cloud.alibaba.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.alibaba.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author what's up
 * @description 针对表【tb_stock(库存表)】的数据库操作Mapper
 * @createDate 2025-07-20 22:18:43
 * @Entity com.cloud.alibaba.stock.module.entity.Stock
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {

}





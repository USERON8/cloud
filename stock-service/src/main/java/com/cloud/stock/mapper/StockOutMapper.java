package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockOut;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author what's up
 * @description 针对表【stock_out(出库明细表)】的数据库操作Mapper
 * @createDate 2025-08-20 13:09:40
 * @Entity com.cloud.stock.module.entity.StockOut
 */
@Mapper
public interface StockOutMapper extends BaseMapper<StockOut> {

}





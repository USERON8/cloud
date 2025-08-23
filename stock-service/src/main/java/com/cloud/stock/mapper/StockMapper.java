package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author what's up
 * @description 针对表【stock(库存主表（支持高并发）)】的数据库操作Mapper
 * @createDate 2025-08-20 13:09:40
 * @Entity com.cloud.stock.module.entity.Stock
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {
    @Select("select * from stock where product_id = #{productId}")
    Stock getByProductId(Long productId);
}





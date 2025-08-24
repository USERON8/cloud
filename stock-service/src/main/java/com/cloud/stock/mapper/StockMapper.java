package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.stock.module.dto.StockPageQueryDTO;
import com.cloud.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
    /**
     * 根据商品ID获取库存信息
     * @param productId 商品ID
     * @return 库存信息
     */
    Stock getByProductId(@Param("productId") Long productId);
    
    /**
     * 分页查询库存列表
     * @param page 分页对象
     * @param query 查询参数
     * @return 库存分页结果
     */
    Page<Stock> pageQuery(Page<Stock> page, @Param("query") StockPageQueryDTO query);
}

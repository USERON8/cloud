package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import domain.PageResult;
import domain.vo.StockVO;

/**
 * 库存服务接口
 */
public interface StockService extends IService<Stock> {

    /**
     * 根据商品ID查询库存
     */
    Stock getByProductId(Long productId);

    /**
     * 分页查询库存
     */
    PageResult<StockVO> pageQuery(StockPageDTO pageDTO);

}

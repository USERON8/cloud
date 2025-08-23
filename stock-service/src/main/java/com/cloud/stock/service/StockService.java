package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.stock.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.module.entity.Stock;

/**
 * 库存服务接口
 */
public interface StockService extends IService<Stock> {
    /**
     * 根据商品ID查询库存
     *
     * @param productId 商品ID
     * @return 库存对象
     */
    StockVO getByProductId(Long productId);

    /**
     * 分页查询库存
     *
     * @param dto 查询参数
     * @return 库存分页结果
     */
    PageResult<StockVO> getStockPage(StockPageDTO dto);

    boolean updateByProductId(Long productId, Integer quantity);
}
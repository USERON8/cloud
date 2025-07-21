package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.vo.StockVO;
import domain.PageResult;

/**
 * 库存服务接口
 */
public interface StockService extends IService<Stock> {

    /**
     * 扣减库存
     */
    boolean deductStock(Long productId, Integer count);

    /**
     * 增加库存
     */
    boolean addStock(Long productId, Integer count);

    /**
     * 检查库存是否充足
     */
    boolean checkStock(Long productId, Integer count);

    /**
     * 分页查询库存
     *
     * @param pageDTO 分页查询条件
     * @return 分页结果
     */
    PageResult<StockVO> pageStock(StockPageDTO pageDTO);

    /**
     * 根据商品ID获取库存VO
     */
    StockVO getStockVOByProductId(Long productId);
}

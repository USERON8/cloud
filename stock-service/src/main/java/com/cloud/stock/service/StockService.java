package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.module.entity.Stock;

/**
 * 库存服务接口
 */

public interface StockService extends IService<Stock> {

    StockVO getByProductId(Long productId);

    PageResult<StockVO> pageQuery(StockPageDTO pageDTO);
}
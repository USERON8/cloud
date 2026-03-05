package com.cloud.api.stock;

import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.stock.StockVO;

public interface StockFeignClient {

    StockVO getStockByProductId(Long productId);

    OperationResultVO updateStock(Long productId, Integer quantity);
}

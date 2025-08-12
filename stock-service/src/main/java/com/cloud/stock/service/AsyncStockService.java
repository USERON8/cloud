package com.cloud.stock.service;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockStatisticsVO;
import com.cloud.common.domain.vo.StockVO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步库存服务接口
 */
public interface AsyncStockService {

    /**
     * 异步根据商品ID查询库存
     */
    CompletableFuture<StockVO> getByProductIdAsync(Long productId);

    /**
     * 异步分页查询库存
     */
    CompletableFuture<PageResult<StockVO>> pageQueryAsync(StockPageDTO pageDTO);

    /**
     * 异步批量查询库存
     */
    CompletableFuture<List<StockVO>> batchQueryAsync(List<Long> productIds);

    /**
     * 异步查询库存统计信息
     */
    CompletableFuture<StockStatisticsVO> getStatisticsAsync();
}

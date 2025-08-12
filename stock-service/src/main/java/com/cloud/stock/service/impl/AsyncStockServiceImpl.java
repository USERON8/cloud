package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockStatisticsVO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.AsyncStockService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步库存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncStockServiceImpl implements AsyncStockService {

    private final StockService stockService;
    private final StockConverter stockConverter;

    /**
     * 异步根据商品ID查询库存
     */
    @Async("stockQueryExecutor")
    @Override
    public CompletableFuture<StockVO> getByProductIdAsync(Long productId) {
        log.info("异步查询商品库存开始，productId: {}, 线程: {}",
                productId, Thread.currentThread().getName());

        try {
            StockVO stockVO = stockService.getByProductId(productId);

            log.info("异步查询商品库存完成，productId: {}, 线程: {}",
                    productId, Thread.currentThread().getName());

            return CompletableFuture.completedFuture(stockVO);
        } catch (Exception e) {
            log.error("异步查询商品库存失败，productId: {}, 线程: {}",
                    productId, Thread.currentThread().getName(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步分页查询库存
     */
    @Async("stockQueryExecutor")
    @Override
    public CompletableFuture<PageResult<StockVO>> pageQueryAsync(StockPageDTO pageDTO) {
        try {
            PageResult<StockVO> result = stockService.pageQuery(pageDTO);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步批量查询库存
     */
    @Async("stockQueryExecutor")
    @Override
    public CompletableFuture<List<StockVO>> batchQueryAsync(List<Long> productIds) {
        try {
            LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Stock::getProductId, productIds);

            List<Stock> stocks = stockService.list(queryWrapper);
            List<StockVO> stockVOs = stockConverter.toVOList(stocks);

            return CompletableFuture.completedFuture(stockVOs);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步查询库存统计信息
     */
    @Async("commonAsyncExecutor")
    @Override
    public CompletableFuture<StockStatisticsVO> getStatisticsAsync() {
        try {
            StockStatisticsVO statistics = new StockStatisticsVO();
            statistics.setTotalProducts(stockService.count());

            return CompletableFuture.completedFuture(statistics);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
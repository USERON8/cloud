package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.api.stock.StockInternalService;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockStatisticsVO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.utils.PageUtils;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 库存服务实现类
 */
@Slf4j
@DubboService
@Service
@RequiredArgsConstructor
public class StockInternalServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockInternalService {

    private final StockConverter stockConverter;

    /**
     * 根据商品ID查询库存
     */
    @Override
    public StockVO getByProductId(Long productId) {
        log.info("查询商品库存，productId: {}", productId);

        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);

        Stock stock = this.getOne(queryWrapper);
        if (stock == null) {
            log.warn("未找到商品库存信息，productId: {}", productId);
        }

        return stockConverter.toVO(stock);
    }

    @Override
    public PageResult<StockVO> pageQuery(StockPageDTO pageDTO) {
        log.info("分页查询库存，查询条件：{}", pageDTO);

        // 转换为stock模块内部使用的DTO
        StockPageDTO stockPageDTO = stockConverter.copyStockPageDTO(pageDTO);

        // 1. 使用PageUtils创建MyBatis-Plus分页对象
        Page<Stock> page = PageUtils.buildPage(pageDTO);

        // 2. 构建查询条件
        LambdaQueryWrapper<Stock> queryWrapper = stockConverter.buildQueryWrapper(stockPageDTO);

        // 3. 执行分页查询
        Page<Stock> resultPage = this.page(page, queryWrapper);

        // 4. 转换实体列表为VO列表
        List<StockVO> stockVOList = stockConverter.toVOList(resultPage.getRecords());

        // 5. 使用PageResult封装分页结果
        PageResult<StockVO> pageResult = PageResult.of(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                stockVOList
        );

        log.info("分页查询完成，总记录数：{}，当前页：{}，每页大小：{}",
                pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());

        return pageResult;
    }


    @Override
    public CompletableFuture<StockVO> getByProductIdAsync(Long productId) {
        return CompletableFuture.supplyAsync(() -> getByProductId(productId));
    }

    @Override
    public CompletableFuture<PageResult<StockVO>> pageQueryAsync(StockPageDTO pageDTO) {
        return CompletableFuture.supplyAsync(() -> {
            // 转换为stock模块内部使用的DTO
            StockPageDTO stockPageDTO = stockConverter.copyStockPageDTO(pageDTO);

            return pageQuery(stockPageDTO);
        });
    }


    @Override
    public CompletableFuture<List<StockVO>> batchQueryAsync(List<Long> productIds) {
        return CompletableFuture.supplyAsync(() -> {
            LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Stock::getProductId, productIds);
            List<Stock> stocks = list(queryWrapper);
            return stockConverter.toVOList(stocks);
        });
    }

    @Override
    public CompletableFuture<StockStatisticsVO> getStatisticsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            StockStatisticsVO statistics = new StockStatisticsVO();
            statistics.setTotalProducts(count());
            return statistics;
        });
    }
    
}
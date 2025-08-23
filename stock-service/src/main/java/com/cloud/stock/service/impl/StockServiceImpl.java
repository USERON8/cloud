package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.stock.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock>
        implements StockService {
    private final StockMapper stockMapper;
    private final StockConverter stockConverter;
    private final StockLogMessageService stockLogMessageService;

    @Override
    public StockVO getByProductId(Long productId) {
        Stock stock = stockMapper.getByProductId(productId);
        return stockConverter.toVO(stock);
    }

    @Override
    public PageResult<StockVO> getStockPage(StockPageDTO dto) {
        // 1. 构建分页对象 (使用dto中的current和size字段)
        Page<Stock> page = new Page<>(dto.getCurrent(), dto.getSize());

        // 2. 构建动态查询条件
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        // 根据productId查询
        wrapper.eq(dto.getProductId() != null, Stock::getProductId, dto.getProductId())
                // 根据productName模糊查询
                .like(StringUtils.isNotBlank(dto.getProductName()),
                        Stock::getProductName, dto.getProductName())
                // 根据库存数量范围查询
                .between(dto.getMinAvailableCount() != null && dto.getMaxAvailableCount() != null,
                        Stock::getStockQuantity, dto.getMinAvailableCount(), dto.getMaxAvailableCount())
                // 根据更新时间排序
                .orderByDesc(Stock::getUpdatedAt);

        // 3. 执行分页查询
        Page<Stock> stockPage = baseMapper.selectPage(page, wrapper);

        // 4. 转换为 VO 并封装结果
        return PageResult.of(
                stockPage.getCurrent(),
                stockPage.getSize(),
                stockPage.getTotal(),
                stockConverter.toVOList(stockPage.getRecords()) // 实体转VO
        );
    }

    @Override
    public boolean updateByProductId(Long productId, Integer quantity) {
        Stock stock = stockMapper.selectById(productId);
        stock.setStockQuantity(quantity);
        if (stockMapper.updateById(stock) > 0) {
            stockLogMessageService.sendStockChangeMessage(
                    productId, stock.getProductName(),
                    stock.getStockQuantity(), quantity,
                    quantity, 1, "USER"
            );
            return true;
        }
        return false;
    }
}
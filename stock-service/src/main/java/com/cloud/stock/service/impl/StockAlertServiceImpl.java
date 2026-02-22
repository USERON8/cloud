package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockAlertService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final StockService stockService;
    private final StockMapper stockMapper;

    @Override
    public List<Stock> getLowStockProducts() {
        List<Stock> allStocks = stockService.list();
        return allStocks.stream()
                .filter(Stock::isLowStock)
                .collect(Collectors.toList());
    }

    @Override
    public List<Stock> getLowStockProductsByThreshold(Integer threshold) {
        int effectiveThreshold = threshold == null || threshold < 0 ? 0 : threshold;
        List<Stock> allStocks = stockService.list();
        return allStocks.stream()
                .filter(stock -> {
                    Integer available = stock.getAvailableQuantity();
                    return available != null && available <= effectiveThreshold;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLowStockThreshold(Long productId, Integer threshold) {
        if (productId == null) {
            throw new BusinessException("productId is required");
        }
        if (threshold == null || threshold < 0) {
            throw new BusinessException("threshold must be >= 0");
        }

        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getProductId, productId));
        if (stock == null) {
            throw new BusinessException("Stock not found by productId");
        }

        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Stock::getProductId, productId)
                .set(Stock::getLowStockThreshold, threshold);
        return stockService.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateLowStockThreshold(List<Long> productIds, Integer threshold) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException("productIds cannot be empty");
        }
        if (threshold == null || threshold < 0) {
            throw new BusinessException("threshold must be >= 0");
        }

        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Stock::getProductId, productIds)
                .set(Stock::getLowStockThreshold, threshold);
        boolean success = stockService.update(updateWrapper);
        return success ? productIds.size() : 0;
    }

    @Override
    public int checkAndSendLowStockAlerts() {
        List<Stock> lowStockProducts = getLowStockProducts();
        if (lowStockProducts.isEmpty()) {
            return 0;
        }
        batchSendLowStockAlerts(lowStockProducts);
        return lowStockProducts.size();
    }

    @Override
    public void sendLowStockAlert(Stock stock) {
        if (stock == null) {
            return;
        }
        log.warn("Low stock alert: productId={}, productName={}, available={}, threshold={}",
                stock.getProductId(),
                stock.getProductName(),
                stock.getAvailableQuantity(),
                stock.getLowStockThreshold());
    }

    @Override
    public void batchSendLowStockAlerts(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        for (Stock stock : stocks) {
            try {
                sendLowStockAlert(stock);
            } catch (Exception e) {
                log.error("Send low stock alert failed: productId={}", stock.getProductId(), e);
            }
        }
    }
}

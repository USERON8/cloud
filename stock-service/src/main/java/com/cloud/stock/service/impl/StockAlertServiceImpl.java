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

        
        List<Stock> lowStockProducts = allStocks.stream()
                .filter(Stock::isLowStock)
                .collect(Collectors.toList());

        
        return lowStockProducts;
    }

    @Override
    public List<Stock> getLowStockProductsByThreshold(Integer threshold) {
        

        }

        
        List<Stock> allStocks = stockService.list();

        
        List<Stock> lowStockProducts = allStocks.stream()
                .filter(stock -> {
                    Integer available = stock.getAvailableQuantity();
                    return available != null && available <= threshold;
                })
                .collect(Collectors.toList());

        
        return lowStockProducts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLowStockThreshold(Long productId, Integer threshold) {
        

        if (productId == null) {
            throw new BusinessException("鍟嗗搧ID涓嶈兘涓虹┖");
        }

        if (threshold == null || threshold < 0) {
            throw new BusinessException("搴撳瓨棰勮闃堝€兼棤鏁?);
        }

        
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);
        Stock stock = stockMapper.selectOne(queryWrapper);
        if (stock == null) {
            throw new BusinessException("鍟嗗搧搴撳瓨涓嶅瓨鍦?);
        }

        
        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Stock::getProductId, productId)
                .set(Stock::getLowStockThreshold, threshold);

        boolean success = stockService.update(updateWrapper);

        if (success) {
            
        } else {
            log.warn("鏇存柊搴撳瓨棰勮闃堝€煎け璐? productId: {}", productId);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateLowStockThreshold(List<Long> productIds, Integer threshold) {
        

        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException("鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖");
        }

        if (threshold == null || threshold < 0) {
            throw new BusinessException("搴撳瓨棰勮闃堝€兼棤鏁?);
        }

        
        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Stock::getProductId, productIds)
                .set(Stock::getLowStockThreshold, threshold);

        boolean success = stockService.update(updateWrapper);

        int count = success ? productIds.size() : 0;
        

        return count;
    }

    @Override
    public int checkAndSendLowStockAlerts() {
        

        try {
            
            List<Stock> lowStockProducts = getLowStockProducts();

            if (lowStockProducts.isEmpty()) {
                return 0;
            }

            
            batchSendLowStockAlerts(lowStockProducts);

            
            return lowStockProducts.size();

        } catch (Exception e) {
            log.error("妫€鏌ヤ綆搴撳瓨棰勮澶辫触", e);
            throw new BusinessException("妫€鏌ヤ綆搴撳瓨棰勮澶辫触", e);
        }
    }

    @Override
    public void sendLowStockAlert(Stock stock) {
        if (stock == null) {
            return;
        }

        log.warn("鈿狅笍 浣庡簱瀛橀璀? 鍟嗗搧: {}, ID: {}, 鍙敤搴撳瓨: {}, 棰勮闃堝€? {}",
                stock.getProductName(),
                stock.getProductId(),
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
                log.error("鍙戦€佷綆搴撳瓨棰勮澶辫触, productId: {}", stock.getProductId(), e);
            }
        }
    }
}

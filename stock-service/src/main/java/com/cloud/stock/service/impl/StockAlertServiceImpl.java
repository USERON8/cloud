package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloud.common.exception.BusinessException;
<<<<<<< HEAD
import com.cloud.stock.mapper.StockMapper;
=======
>>>>>>> e21a1f7d9b92cc459b064effcfce34c80c2fd3b8
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockAlertService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存预警服务实现
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final StockService stockService;
<<<<<<< HEAD
    private final StockMapper stockMapper;
=======
>>>>>>> e21a1f7d9b92cc459b064effcfce34c80c2fd3b8

    @Override
    public List<Stock> getLowStockProducts() {
        log.info("查询所有低库存商品");

        // 查询所有库存
        List<Stock> allStocks = stockService.list();

        // 过滤出低库存商品
        List<Stock> lowStockProducts = allStocks.stream()
                .filter(Stock::isLowStock)
                .collect(Collectors.toList());

        log.info("低库存商品数量: {}", lowStockProducts.size());
        return lowStockProducts;
    }

    @Override
    public List<Stock> getLowStockProductsByThreshold(Integer threshold) {
        log.info("查询库存低于 {} 的商品", threshold);

        if (threshold == null || threshold < 0) {
            throw new BusinessException("库存阈值无效");
        }

        // 查询所有库存
        List<Stock> allStocks = stockService.list();

        // 过滤出可用库存低于阈值的商品
        List<Stock> lowStockProducts = allStocks.stream()
                .filter(stock -> {
                    Integer available = stock.getAvailableQuantity();
                    return available != null && available <= threshold;
                })
                .collect(Collectors.toList());

        log.info("库存低于 {} 的商品数量: {}", threshold, lowStockProducts.size());
        return lowStockProducts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLowStockThreshold(Long productId, Integer threshold) {
        log.info("更新商品库存预警阈值, productId: {}, threshold: {}", productId, threshold);

        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }

        if (threshold == null || threshold < 0) {
            throw new BusinessException("库存预警阈值无效");
        }

        // 检查商品库存是否存在
<<<<<<< HEAD
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);
        Stock stock = stockMapper.selectOne(queryWrapper);
=======
        Stock stock = stockService.getStockByProductId(productId);
>>>>>>> e21a1f7d9b92cc459b064effcfce34c80c2fd3b8
        if (stock == null) {
            throw new BusinessException("商品库存不存在");
        }

        // 更新预警阈值
        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Stock::getProductId, productId)
                .set(Stock::getLowStockThreshold, threshold);

        boolean success = stockService.update(updateWrapper);

        if (success) {
            log.info("更新库存预警阈值成功, productId: {}, threshold: {}", productId, threshold);
        } else {
            log.warn("更新库存预警阈值失败, productId: {}", productId);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateLowStockThreshold(List<Long> productIds, Integer threshold) {
        log.info("批量更新库存预警阈值, 商品数量: {}, threshold: {}", productIds.size(), threshold);

        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException("商品ID列表不能为空");
        }

        if (threshold == null || threshold < 0) {
            throw new BusinessException("库存预警阈值无效");
        }

        // 批量更新预警阈值
        LambdaUpdateWrapper<Stock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Stock::getProductId, productIds)
                .set(Stock::getLowStockThreshold, threshold);

        boolean success = stockService.update(updateWrapper);

        int count = success ? productIds.size() : 0;
        log.info("批量更新库存预警阈值完成, 成功数量: {}", count);

        return count;
    }

    @Override
    public int checkAndSendLowStockAlerts() {
        log.info("开始检查低库存并发送预警通知");

        try {
            // 获取所有低库存商品
            List<Stock> lowStockProducts = getLowStockProducts();

            if (lowStockProducts.isEmpty()) {
                log.info("当前没有低库存商品");
                return 0;
            }

            // 批量发送预警通知
            batchSendLowStockAlerts(lowStockProducts);

            log.info("低库存预警检查完成, 预警商品数量: {}", lowStockProducts.size());
            return lowStockProducts.size();

        } catch (Exception e) {
            log.error("检查低库存预警失败", e);
            throw new BusinessException("检查低库存预警失败", e);
        }
    }

    @Override
    public void sendLowStockAlert(Stock stock) {
        if (stock == null) {
            return;
        }

        log.warn("⚠️ 低库存预警! 商品: {}, ID: {}, 可用库存: {}, 预警阈值: {}",
                stock.getProductName(),
                stock.getProductId(),
                stock.getAvailableQuantity(),
                stock.getLowStockThreshold());

        // TODO: 这里可以集成RocketMQ发送通知消息
        // TODO: 可以发送邮件、短信、站内消息等
        // 示例: rocketMQTemplate.convertAndSend("stock-alert-topic", alertMessage);
    }

    @Override
    public void batchSendLowStockAlerts(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return;
        }

        log.info("批量发送低库存预警通知, 数量: {}", stocks.size());

        for (Stock stock : stocks) {
            try {
                sendLowStockAlert(stock);
            } catch (Exception e) {
                log.error("发送低库存预警失败, productId: {}", stock.getProductId(), e);
            }
        }
    }
}

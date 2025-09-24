package com.cloud.stock.service.impl;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 库存注解服务实现类
 * 演示如何使用@DistributedLock注解进行声明式分布式锁
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnnotationServiceImpl {

    private final StockMapper stockMapper;
    private final StockConverter stockConverter;

    /**
     * 使用注解的库存出库操作
     * 演示基本的分布式锁使用
     *
     * @param productId 商品ID
     * @param quantity  出库数量
     * @return 是否成功
     */
    @DistributedLock(
            key = "'stock:product:' + #productId",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存出库操作获取锁失败"
    )
    @Transactional
    public boolean stockOutWithAnnotation(Long productId, Integer quantity) {
        log.info("🔄 开始库存出库操作 - 商品ID: {}, 数量: {}", productId, quantity);

        // 模拟业务处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 执行条件出库
        int affectedRows = stockMapper.stockOutWithCondition(productId, quantity);

        boolean success = affectedRows > 0;
        log.info("✅ 库存出库操作完成 - 商品ID: {}, 数量: {}, 结果: {}",
                productId, quantity, success ? "成功" : "失败");

        return success;
    }

    /**
     * 使用注解的库存预留操作
     * 演示公平锁的使用
     *
     * @param productId 商品ID
     * @param quantity  预留数量
     * @return 是否成功
     */
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 3,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_DEFAULT,
            failMessage = "库存预留操作获取公平锁失败"
    )
    @Transactional
    public boolean reserveStockWithAnnotation(Long productId, Integer quantity) {
        log.info("🔄 开始库存预留操作 - 商品ID: {}, 数量: {}", productId, quantity);

        // 模拟业务处理时间
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 执行条件预留
        int affectedRows = stockMapper.reserveStockWithCondition(productId, quantity);

        boolean success = affectedRows > 0;
        log.info("✅ 库存预留操作完成 - 商品ID: {}, 数量: {}, 结果: {}",
                productId, quantity, success ? "成功" : "失败");

        return success;
    }

    /**
     * 使用注解的库存查询操作
     * 演示读锁的使用
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @DistributedLock(
            key = "'stock:query:' + #productId",
            lockType = DistributedLock.LockType.READ,
            waitTime = 2,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    @Transactional(readOnly = true)
    public StockDTO getStockWithAnnotation(Long productId) {
        log.info("🔍 开始查询库存信息 - 商品ID: {}", productId);

        // 模拟查询处理时间
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        if (stock == null) {
            log.warn("⚠️ 库存信息不存在 - 商品ID: {}", productId);
            return null;
        }

        StockDTO stockDTO = stockConverter.toDTO(stock);
        log.info("✅ 库存查询完成 - 商品ID: {}, 库存数量: {}, 冻结数量: {}",
                productId, stock.getStockQuantity(), stock.getFrozenQuantity());

        return stockDTO;
    }

    /**
     * 使用注解的库存更新操作
     * 演示写锁的使用
     *
     * @param productId   商品ID
     * @param newQuantity 新的库存数量
     * @return 是否成功
     */
    @DistributedLock(
            key = "'stock:update:' + #productId",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 5,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST
    )
    @Transactional
    public boolean updateStockWithAnnotation(Long productId, Integer newQuantity) {
        log.info("🔄 开始更新库存数量 - 商品ID: {}, 新数量: {}", productId, newQuantity);

        // 模拟业务处理时间
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 查询当前库存
        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        if (stock == null) {
            log.warn("⚠️ 库存信息不存在 - 商品ID: {}", productId);
            return false;
        }

        // 更新库存数量
        stock.setStockQuantity(newQuantity);
        int affectedRows = stockMapper.updateById(stock);

        boolean success = affectedRows > 0;
        log.info("✅ 库存更新完成 - 商品ID: {}, 新数量: {}, 结果: {}",
                productId, newQuantity, success ? "成功" : "失败");

        return success;
    }

    /**
     * 使用注解的批量库存操作
     * 演示复杂SpEL表达式的使用
     *
     * @param productIds 商品ID列表
     * @param operation  操作类型
     * @return 处理数量
     */
    @DistributedLock(
            key = "'stock:batch:' + #operation + ':' + T(String).join(',', #productIds)",
            prefix = "batch",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "批量库存操作获取锁失败"
    )
    @Transactional
    public int batchStockOperationWithAnnotation(java.util.List<Long> productIds, String operation) {
        log.info("🔄 开始批量库存操作 - 操作类型: {}, 商品数量: {}", operation, productIds.size());

        int processedCount = 0;

        for (Long productId : productIds) {
            try {
                // 模拟处理每个商品
                Thread.sleep(10);

                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock != null) {
                    // 根据操作类型执行不同逻辑
                    switch (operation) {
                        case "refresh" -> {
                            // 刷新库存逻辑
                            stockMapper.updateById(stock);
                            processedCount++;
                        }
                        case "check" -> {
                            // 检查库存逻辑
                            if (stock.getStockQuantity() > 0) {
                                processedCount++;
                            }
                        }
                        default -> log.warn("⚠️ 未知操作类型: {}", operation);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ 处理商品库存异常 - 商品ID: {}", productId, e);
            }
        }

        log.info("✅ 批量库存操作完成 - 操作类型: {}, 处理数量: {}/{}",
                operation, processedCount, productIds.size());

        return processedCount;
    }

    /**
     * 演示锁获取失败时返回null的情况
     *
     * @param productId 商品ID
     * @return 库存信息或null
     */
    @DistributedLock(
            key = "'stock:safe:' + #productId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL,
            failMessage = "快速查询库存获取锁失败"
    )
    public StockDTO quickGetStockWithAnnotation(Long productId) {
        log.info("⚡ 快速查询库存信息 - 商品ID: {}", productId);

        Stock stock = stockMapper.selectById(productId);
        return stock != null ? stockConverter.toDTO(stock) : null;
    }
}

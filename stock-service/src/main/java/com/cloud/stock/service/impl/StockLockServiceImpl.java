package com.cloud.stock.service.impl;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.lock.DistributedLockTemplate;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.dto.StockOperationResult;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 库存锁服务实现类
 * 基于分布式锁实现的库存操作服务，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockLockServiceImpl implements StockLockService {

    /**
     * 锁超时时间（秒）
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    /**
     * 锁等待时间（毫秒）
     */
    private static final Duration LOCK_WAIT_TIME = Duration.ofMillis(500);
    private final StockMapper stockMapper;
    private final StockConverter stockConverter;
    private final DistributedLockTemplate lockTemplate;

    @Override
    @Transactional
    public StockOperationResult safeStockOut(Long productId, Integer quantity, Long operatorId, String remark) {
        validateParameters(productId, quantity, operatorId);

        String lockKey = buildStockLockKey(productId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前库存
                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock == null) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.STOCK_OUT,
                            productId, quantity,
                            StockOperationResult.ErrorCode.STOCK_NOT_FOUND,
                            "库存信息不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStock = stock.getStockQuantity();
                Integer beforeFrozen = stock.getFrozenQuantity();

                // 执行条件出库
                int affectedRows = stockMapper.stockOutWithCondition(productId, quantity);

                if (affectedRows == 0) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.STOCK_OUT,
                            productId, quantity,
                            StockOperationResult.ErrorCode.INSUFFICIENT_STOCK,
                            String.format("库存不足，当前可用库存: %d，需要出库: %d",
                                    beforeStock - beforeFrozen, quantity),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询更新后的库存
                Stock updatedStock = stockMapper.selectByProductIdForUpdate(productId);
                Integer afterStock = updatedStock.getStockQuantity();
                Integer afterFrozen = updatedStock.getFrozenQuantity();

                log.info("✅ 库存出库成功 - 商品ID: {}, 出库数量: {}, 库存变化: {} -> {}",
                        productId, quantity, beforeStock, afterStock);

                return StockOperationResult.success(
                        StockOperationResult.OperationType.STOCK_OUT,
                        productId, quantity,
                        beforeStock, afterStock,
                        beforeFrozen, afterFrozen,
                        operatorId, remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 库存出库异常 - 商品ID: {}, 数量: {}", productId, quantity, e);
                return StockOperationResult.failure(
                        StockOperationResult.OperationType.STOCK_OUT,
                        productId, quantity,
                        StockOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public StockOperationResult safeReserveStock(Long productId, Integer quantity, Long operatorId, String remark) {
        validateParameters(productId, quantity, operatorId);

        String lockKey = buildStockLockKey(productId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前库存
                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock == null) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.RESERVE,
                            productId, quantity,
                            StockOperationResult.ErrorCode.STOCK_NOT_FOUND,
                            "库存信息不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStock = stock.getStockQuantity();
                Integer beforeFrozen = stock.getFrozenQuantity();

                // 执行条件预留
                int affectedRows = stockMapper.reserveStockWithCondition(productId, quantity);

                if (affectedRows == 0) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.RESERVE,
                            productId, quantity,
                            StockOperationResult.ErrorCode.INSUFFICIENT_STOCK,
                            String.format("库存不足，当前可用库存: %d，需要预留: %d",
                                    beforeStock - beforeFrozen, quantity),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询更新后的库存
                Stock updatedStock = stockMapper.selectByProductIdForUpdate(productId);
                Integer afterStock = updatedStock.getStockQuantity();
                Integer afterFrozen = updatedStock.getFrozenQuantity();

                log.info("✅ 库存预留成功 - 商品ID: {}, 预留数量: {}, 冻结库存变化: {} -> {}",
                        productId, quantity, beforeFrozen, afterFrozen);

                return StockOperationResult.success(
                        StockOperationResult.OperationType.RESERVE,
                        productId, quantity,
                        beforeStock, afterStock,
                        beforeFrozen, afterFrozen,
                        operatorId, remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 库存预留异常 - 商品ID: {}, 数量: {}", productId, quantity, e);
                return StockOperationResult.failure(
                        StockOperationResult.OperationType.RESERVE,
                        productId, quantity,
                        StockOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public StockOperationResult safeReleaseReservedStock(Long productId, Integer quantity, Long operatorId, String remark) {
        validateParameters(productId, quantity, operatorId);

        String lockKey = buildStockLockKey(productId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前库存
                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock == null) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.RELEASE_RESERVE,
                            productId, quantity,
                            StockOperationResult.ErrorCode.STOCK_NOT_FOUND,
                            "库存信息不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStock = stock.getStockQuantity();
                Integer beforeFrozen = stock.getFrozenQuantity();

                // 执行条件释放预留
                int affectedRows = stockMapper.releaseReservedStockWithCondition(productId, quantity);

                if (affectedRows == 0) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.RELEASE_RESERVE,
                            productId, quantity,
                            StockOperationResult.ErrorCode.INSUFFICIENT_FROZEN,
                            String.format("冻结库存不足，当前冻结库存: %d，需要释放: %d", beforeFrozen, quantity),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询更新后的库存
                Stock updatedStock = stockMapper.selectByProductIdForUpdate(productId);
                Integer afterStock = updatedStock.getStockQuantity();
                Integer afterFrozen = updatedStock.getFrozenQuantity();

                log.info("✅ 预留库存释放成功 - 商品ID: {}, 释放数量: {}, 冻结库存变化: {} -> {}",
                        productId, quantity, beforeFrozen, afterFrozen);

                return StockOperationResult.success(
                        StockOperationResult.OperationType.RELEASE_RESERVE,
                        productId, quantity,
                        beforeStock, afterStock,
                        beforeFrozen, afterFrozen,
                        operatorId, remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 预留库存释放异常 - 商品ID: {}, 数量: {}", productId, quantity, e);
                return StockOperationResult.failure(
                        StockOperationResult.OperationType.RELEASE_RESERVE,
                        productId, quantity,
                        StockOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public StockOperationResult safeConfirmStockOut(Long productId, Integer quantity, Long operatorId, String remark) {
        validateParameters(productId, quantity, operatorId);

        String lockKey = buildStockLockKey(productId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前库存
                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock == null) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.CONFIRM_OUT,
                            productId, quantity,
                            StockOperationResult.ErrorCode.STOCK_NOT_FOUND,
                            "库存信息不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStock = stock.getStockQuantity();
                Integer beforeFrozen = stock.getFrozenQuantity();

                // 执行条件确认出库
                int affectedRows = stockMapper.confirmStockOutWithCondition(productId, quantity);

                if (affectedRows == 0) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.CONFIRM_OUT,
                            productId, quantity,
                            StockOperationResult.ErrorCode.INSUFFICIENT_FROZEN,
                            String.format("冻结库存不足，当前冻结库存: %d，需要确认出库: %d", beforeFrozen, quantity),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询更新后的库存
                Stock updatedStock = stockMapper.selectByProductIdForUpdate(productId);
                Integer afterStock = updatedStock.getStockQuantity();
                Integer afterFrozen = updatedStock.getFrozenQuantity();

                log.info("✅ 确认出库成功 - 商品ID: {}, 确认数量: {}, 库存变化: {} -> {}, 冻结库存变化: {} -> {}",
                        productId, quantity, beforeStock, afterStock, beforeFrozen, afterFrozen);

                return StockOperationResult.success(
                        StockOperationResult.OperationType.CONFIRM_OUT,
                        productId, quantity,
                        beforeStock, afterStock,
                        beforeFrozen, afterFrozen,
                        operatorId, remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 确认出库异常 - 商品ID: {}, 数量: {}", productId, quantity, e);
                return StockOperationResult.failure(
                        StockOperationResult.OperationType.CONFIRM_OUT,
                        productId, quantity,
                        StockOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public StockOperationResult safeStockIn(Long productId, Integer quantity, Long operatorId, String remark) {
        validateParameters(productId, quantity, operatorId);

        String lockKey = buildStockLockKey(productId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前库存
                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock == null) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.STOCK_IN,
                            productId, quantity,
                            StockOperationResult.ErrorCode.STOCK_NOT_FOUND,
                            "库存信息不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStock = stock.getStockQuantity();
                Integer beforeFrozen = stock.getFrozenQuantity();

                // 执行条件入库
                int affectedRows = stockMapper.stockInWithCondition(productId, quantity);

                if (affectedRows == 0) {
                    return StockOperationResult.failure(
                            StockOperationResult.OperationType.STOCK_IN,
                            productId, quantity,
                            StockOperationResult.ErrorCode.CONCURRENT_UPDATE_FAILED,
                            "并发更新失败，请重试",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询更新后的库存
                Stock updatedStock = stockMapper.selectByProductIdForUpdate(productId);
                Integer afterStock = updatedStock.getStockQuantity();
                Integer afterFrozen = updatedStock.getFrozenQuantity();

                log.info("✅ 库存入库成功 - 商品ID: {}, 入库数量: {}, 库存变化: {} -> {}",
                        productId, quantity, beforeStock, afterStock);

                return StockOperationResult.success(
                        StockOperationResult.OperationType.STOCK_IN,
                        productId, quantity,
                        beforeStock, afterStock,
                        beforeFrozen, afterFrozen,
                        operatorId, remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 库存入库异常 - 商品ID: {}, 数量: {}", productId, quantity, e);
                return StockOperationResult.failure(
                        StockOperationResult.OperationType.STOCK_IN,
                        productId, quantity,
                        StockOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public StockDTO getStockWithLock(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        String lockKey = buildStockLockKey(productId);

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            Stock stock = stockMapper.selectByProductIdForUpdate(productId);
            if (stock == null) {
                throw new EntityNotFoundException("库存信息不存在，商品ID: " + productId);
            }
            return stockConverter.toDTO(stock);
        });
    }

    /**
     * 参数验证
     *
     * @param productId  商品ID
     * @param quantity   数量
     * @param operatorId 操作人ID
     */
    private void validateParameters(Long productId, Integer quantity, Long operatorId) {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
    }

    /**
     * 构建库存锁键
     *
     * @param productId 商品ID
     * @return 锁键
     */
    private String buildStockLockKey(Long productId) {
        return "stock:product:" + productId;
    }
}

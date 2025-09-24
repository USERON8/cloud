package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.lock.DistributedLockTemplate;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.dto.OrderStatusChangeResult;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 订单锁服务实现类
 * 基于分布式锁实现的订单状态机操作服务，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLockServiceImpl implements OrderLockService {

    /**
     * 锁超时时间（秒）
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    /**
     * 锁等待时间（毫秒）
     */
    private static final Duration LOCK_WAIT_TIME = Duration.ofMillis(500);
    private final OrderMapper orderMapper;
    private final OrderConverter orderConverter;
    private final DistributedLockTemplate lockTemplate;

    @Override
    @Transactional
    public OrderStatusChangeResult safePayOrder(Long orderId, Long operatorId, String remark) {
        validateParameters(orderId, operatorId);

        String lockKey = buildOrderLockKey(orderId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前订单状态
                Order order = orderMapper.selectByIdForUpdate(orderId);
                if (order == null) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.PAY_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.ORDER_NOT_FOUND,
                            "订单不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = order.getStatus();

                // 执行条件状态更新
                int affectedRows = orderMapper.updateStatusToPaid(orderId);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.PAY_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("订单状态不允许支付，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 订单支付成功 - 订单ID: {}, 状态变化: {} -> {}",
                        orderId, getStatusName(beforeStatus), getStatusName(OrderStatusChangeResult.OrderStatus.PAID));

                return OrderStatusChangeResult.success(
                        OrderStatusChangeResult.OperationType.PAY_ORDER,
                        orderId,
                        beforeStatus,
                        OrderStatusChangeResult.OrderStatus.PAID,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 订单支付异常 - 订单ID: {}", orderId, e);
                return OrderStatusChangeResult.failure(
                        OrderStatusChangeResult.OperationType.PAY_ORDER,
                        orderId,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public OrderStatusChangeResult safeShipOrder(Long orderId, Long operatorId, String remark) {
        validateParameters(orderId, operatorId);

        String lockKey = buildOrderLockKey(orderId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前订单状态
                Order order = orderMapper.selectByIdForUpdate(orderId);
                if (order == null) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.SHIP_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.ORDER_NOT_FOUND,
                            "订单不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = order.getStatus();

                // 执行条件状态更新
                int affectedRows = orderMapper.updateStatusToShipped(orderId);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.SHIP_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("订单状态不允许发货，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 订单发货成功 - 订单ID: {}, 状态变化: {} -> {}",
                        orderId, getStatusName(beforeStatus), getStatusName(OrderStatusChangeResult.OrderStatus.SHIPPED));

                return OrderStatusChangeResult.success(
                        OrderStatusChangeResult.OperationType.SHIP_ORDER,
                        orderId,
                        beforeStatus,
                        OrderStatusChangeResult.OrderStatus.SHIPPED,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 订单发货异常 - 订单ID: {}", orderId, e);
                return OrderStatusChangeResult.failure(
                        OrderStatusChangeResult.OperationType.SHIP_ORDER,
                        orderId,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public OrderStatusChangeResult safeCompleteOrder(Long orderId, Long operatorId, String remark) {
        validateParameters(orderId, operatorId);

        String lockKey = buildOrderLockKey(orderId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前订单状态
                Order order = orderMapper.selectByIdForUpdate(orderId);
                if (order == null) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.COMPLETE_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.ORDER_NOT_FOUND,
                            "订单不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = order.getStatus();

                // 执行条件状态更新
                int affectedRows = orderMapper.updateStatusToCompleted(orderId);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.COMPLETE_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("订单状态不允许完成，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 订单完成成功 - 订单ID: {}, 状态变化: {} -> {}",
                        orderId, getStatusName(beforeStatus), getStatusName(OrderStatusChangeResult.OrderStatus.COMPLETED));

                return OrderStatusChangeResult.success(
                        OrderStatusChangeResult.OperationType.COMPLETE_ORDER,
                        orderId,
                        beforeStatus,
                        OrderStatusChangeResult.OrderStatus.COMPLETED,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 订单完成异常 - 订单ID: {}", orderId, e);
                return OrderStatusChangeResult.failure(
                        OrderStatusChangeResult.OperationType.COMPLETE_ORDER,
                        orderId,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public OrderStatusChangeResult safeCancelPendingOrder(Long orderId, Long operatorId, String remark) {
        validateParameters(orderId, operatorId);

        String lockKey = buildOrderLockKey(orderId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前订单状态
                Order order = orderMapper.selectByIdForUpdate(orderId);
                if (order == null) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.CANCEL_PENDING_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.ORDER_NOT_FOUND,
                            "订单不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = order.getStatus();

                // 执行条件状态更新
                int affectedRows = orderMapper.cancelOrderFromPending(orderId);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.CANCEL_PENDING_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("订单状态不允许取消，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 待支付订单取消成功 - 订单ID: {}, 状态变化: {} -> {}",
                        orderId, getStatusName(beforeStatus), getStatusName(OrderStatusChangeResult.OrderStatus.CANCELLED));

                return OrderStatusChangeResult.success(
                        OrderStatusChangeResult.OperationType.CANCEL_PENDING_ORDER,
                        orderId,
                        beforeStatus,
                        OrderStatusChangeResult.OrderStatus.CANCELLED,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 待支付订单取消异常 - 订单ID: {}", orderId, e);
                return OrderStatusChangeResult.failure(
                        OrderStatusChangeResult.OperationType.CANCEL_PENDING_ORDER,
                        orderId,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public OrderStatusChangeResult safeCancelPaidOrder(Long orderId, Long operatorId, String remark) {
        validateParameters(orderId, operatorId);

        String lockKey = buildOrderLockKey(orderId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前订单状态
                Order order = orderMapper.selectByIdForUpdate(orderId);
                if (order == null) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.CANCEL_PAID_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.ORDER_NOT_FOUND,
                            "订单不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = order.getStatus();

                // 执行条件状态更新
                int affectedRows = orderMapper.cancelOrderFromPaid(orderId);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.failure(
                            OrderStatusChangeResult.OperationType.CANCEL_PAID_ORDER,
                            orderId,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("订单状态不允许取消，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 已支付订单取消成功 - 订单ID: {}, 状态变化: {} -> {}",
                        orderId, getStatusName(beforeStatus), getStatusName(OrderStatusChangeResult.OrderStatus.CANCELLED));

                return OrderStatusChangeResult.success(
                        OrderStatusChangeResult.OperationType.CANCEL_PAID_ORDER,
                        orderId,
                        beforeStatus,
                        OrderStatusChangeResult.OrderStatus.CANCELLED,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 已支付订单取消异常 - 订单ID: {}", orderId, e);
                return OrderStatusChangeResult.failure(
                        OrderStatusChangeResult.OperationType.CANCEL_PAID_ORDER,
                        orderId,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderWithLock(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }

        String lockKey = buildOrderLockKey(orderId);

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            Order order = orderMapper.selectByIdForUpdate(orderId);
            if (order == null) {
                throw new EntityNotFoundException("订单不存在，订单ID: " + orderId);
            }
            return orderConverter.toDTO(order);
        });
    }

    @Override
    @Transactional
    public OrderStatusChangeResult batchSafeStatusChange(List<Long> orderIds, Integer fromStatus,
                                                         Integer toStatus, Long operatorId, String remark) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("订单ID列表不能为空");
        }
        if (fromStatus == null || toStatus == null) {
            throw new IllegalArgumentException("状态参数不能为空");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }

        // 对订单ID排序，避免死锁
        List<Long> sortedOrderIds = orderIds.stream().sorted().toList();
        String lockKey = buildBatchOrderLockKey(sortedOrderIds);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 执行批量状态更新
                int affectedRows = orderMapper.batchUpdateStatus(sortedOrderIds, fromStatus, toStatus);

                if (affectedRows == 0) {
                    return OrderStatusChangeResult.batchFailure(
                            OrderStatusChangeResult.OperationType.BATCH_STATUS_CHANGE,
                            sortedOrderIds,
                            OrderStatusChangeResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("批量状态变更失败，原状态: %s，目标状态: %s",
                                    getStatusName(fromStatus), getStatusName(toStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 批量订单状态变更成功 - 订单数量: {}, 影响行数: {}, 状态变化: {} -> {}",
                        sortedOrderIds.size(), affectedRows, getStatusName(fromStatus), getStatusName(toStatus));

                return OrderStatusChangeResult.batchSuccess(
                        OrderStatusChangeResult.OperationType.BATCH_STATUS_CHANGE,
                        sortedOrderIds,
                        fromStatus,
                        toStatus,
                        affectedRows,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 批量订单状态变更异常 - 订单数量: {}", sortedOrderIds.size(), e);
                return OrderStatusChangeResult.batchFailure(
                        OrderStatusChangeResult.OperationType.BATCH_STATUS_CHANGE,
                        sortedOrderIds,
                        OrderStatusChangeResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    /**
     * 参数验证
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     */
    private void validateParameters(Long orderId, Long operatorId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
    }

    /**
     * 构建订单锁键
     *
     * @param orderId 订单ID
     * @return 锁键
     */
    private String buildOrderLockKey(Long orderId) {
        return "order:status:" + orderId;
    }

    /**
     * 构建批量订单锁键
     *
     * @param orderIds 订单ID列表（已排序）
     * @return 锁键
     */
    private String buildBatchOrderLockKey(List<Long> orderIds) {
        return "order:batch:" + String.join(",", orderIds.stream().map(String::valueOf).toArray(String[]::new));
    }

    /**
     * 获取状态名称
     *
     * @param status 状态码
     * @return 状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知状态(" + status + ")";
        };
    }
}

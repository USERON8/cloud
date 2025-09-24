package com.cloud.order.service;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.module.dto.OrderStatusChangeResult;

/**
 * 订单锁服务接口
 * 提供基于分布式锁的订单状态机操作，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface OrderLockService {

    /**
     * 安全支付订单 - 使用分布式锁保护
     * 从待支付状态转换为已支付状态
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult safePayOrder(Long orderId, Long operatorId, String remark);

    /**
     * 安全发货订单 - 使用分布式锁保护
     * 从已支付状态转换为已发货状态
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult safeShipOrder(Long orderId, Long operatorId, String remark);

    /**
     * 安全完成订单 - 使用分布式锁保护
     * 从已发货状态转换为已完成状态
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult safeCompleteOrder(Long orderId, Long operatorId, String remark);

    /**
     * 安全取消订单（待支付） - 使用分布式锁保护
     * 从待支付状态转换为已取消状态
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult safeCancelPendingOrder(Long orderId, Long operatorId, String remark);

    /**
     * 安全取消订单（已支付） - 使用分布式锁保护
     * 从已支付状态转换为已取消状态（退款场景）
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult safeCancelPaidOrder(Long orderId, Long operatorId, String remark);

    /**
     * 获取订单信息 - 使用分布式锁保护
     * 确保获取到的订单信息是最新的
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    OrderDTO getOrderWithLock(Long orderId);

    /**
     * 批量安全状态变更 - 使用分布式锁保护
     * 支持批量订单状态转换，保证原子性
     *
     * @param orderIds   订单ID列表
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 状态变更结果
     */
    OrderStatusChangeResult batchSafeStatusChange(java.util.List<Long> orderIds,
                                                  Integer fromStatus,
                                                  Integer toStatus,
                                                  Long operatorId,
                                                  String remark);
}

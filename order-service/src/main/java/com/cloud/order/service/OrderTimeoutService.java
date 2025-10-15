package com.cloud.order.service;

import com.cloud.order.module.entity.Order;

import java.util.List;

/**
 * 订单超时处理服务接口
 *
 * @author what's up
 */
public interface OrderTimeoutService {
    /**
     * 检查并处理超时未支付订单
     * (用于定时任务调用)
     *
     * @return 处理的订单数量
     */
    int checkAndHandleTimeoutOrders();

    /**
     * 获取超时未支付的订单列表
     *
     * @param timeoutMinutes 超时分钟数(默认30分钟)
     * @return 超时订单列表
     */
    List<Order> getTimeoutOrders(Integer timeoutMinutes);

    /**
     * 取消超时订单
     *
     * @param orderId 订单ID
     * @return 取消结果
     */
    boolean cancelTimeoutOrder(Long orderId);

    /**
     * 批量取消超时订单
     *
     * @param orderIds 订单ID列表
     * @return 成功取消的数量
     */
    int batchCancelTimeoutOrders(List<Long> orderIds);

    /**
     * 获取订单超时配置(分钟数)
     *
     * @return 超时分钟数
     */
    Integer getTimeoutConfig();

    /**
     * 更新订单超时配置
     *
     * @param timeoutMinutes 超时分钟数
     * @return 更新结果
     */
    boolean updateTimeoutConfig(Integer timeoutMinutes);
}

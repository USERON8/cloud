package com.cloud.order.service;

import com.cloud.order.dto.SimpleOrderCreateDTO;

/**
 * 简化订单服务接口
 * 专门处理单商品订单的业务逻辑
 * 简化流程，专注于事件驱动的订单处理
 *
 * @author cloud
 * @since 1.0.0
 */
public interface SimpleOrderService {

    /**
     * 创建单商品订单
     * 简化版订单创建流程，自动发布订单创建事件
     *
     * @param orderCreateDTO 简化订单创建信息
     * @param currentUserId  当前用户ID
     * @return 订单ID
     */
    Long createSimpleOrder(SimpleOrderCreateDTO orderCreateDTO, String currentUserId);

    /**
     * 查询订单状态
     * 返回人性化的订单状态描述
     *
     * @param orderId 订单ID
     * @return 订单状态描述
     */
    String getOrderStatus(Long orderId);

    /**
     * 模拟支付完成
     * 用于测试支付成功后的订单完成流程
     *
     * @param orderId 订单ID
     * @return 是否处理成功
     */
    boolean mockPaymentComplete(Long orderId);

    /**
     * 处理支付成功事件
     * 更新订单状态为已支付，并发布订单完成事件
     *
     * @param orderId       订单ID
     * @param paymentId     支付ID
     * @param paymentAmount 支付金额
     * @return 是否处理成功
     */
    boolean handlePaymentSuccess(Long orderId, Long paymentId, java.math.BigDecimal paymentAmount);
}

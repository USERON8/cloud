package com.cloud.payment.service;

import java.math.BigDecimal;

/**
 * 支付测试服务接口
 * 用于测试支付流程，模拟各种支付场景
 *
 * @author what's up
 * @since 1.0.0
 */
public interface PaymentTestService {

    /**
     * 模拟支付成功
     *
     * @param orderId 订单ID
     * @param amount  支付金额
     * @param channel 支付渠道
     * @return 处理结果
     */
    String simulatePaymentSuccess(Long orderId, BigDecimal amount, Integer channel);

    /**
     * 模拟支付失败
     *
     * @param orderId 订单ID
     * @param reason  失败原因
     * @return 处理结果
     */
    String simulatePaymentFailure(Long orderId, String reason);

    /**
     * 获取支付状态
     *
     * @param orderId 订单ID
     * @return 支付状态信息
     */
    Object getPaymentStatus(Long orderId);

    /**
     * 重置支付状态
     *
     * @param orderId 订单ID
     * @return 处理结果
     */
    String resetPaymentStatus(Long orderId);

    /**
     * 测试完整支付流程
     *
     * @param userId      用户ID
     * @param amount      支付金额
     * @param productName 商品名称
     * @return 流程测试结果
     */
    Object testFullPaymentFlow(Long userId, BigDecimal amount, String productName);
}

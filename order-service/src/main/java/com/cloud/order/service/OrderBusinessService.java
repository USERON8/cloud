package com.cloud.order.service;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.dto.OrderCreateRequestDTO;
import com.cloud.order.enums.OrderStatusEnum;
import com.cloud.order.exception.OrderBusinessException;

/**
 * 订单业务服务接口
 * 定义订单核心业务功能
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
public interface OrderBusinessService {

    /**
     * 创建订单
     * <p>
     * 流程：
     * 1. 验证用户信息和商品信息
     * 2. 创建订单主记录和订单项
     * 3. 发送库存预扣减消息
     * 4. 发送支付记录创建消息
     * 5. 发送订单创建日志消息
     *
     * @param createRequest 订单创建请求
     * @param operatorId    操作人ID
     * @return 创建的订单ID
     * @throws OrderBusinessException 订单业务异常
     */
    Long createOrder(OrderCreateRequestDTO createRequest, Long operatorId);

    /**
     * 处理支付成功事件
     * <p>
     * 流程：
     * 1. 更新订单状态为已支付
     * 2. 记录支付时间
     * 3. 发送库存确认扣减消息
     * 4. 发送订单支付成功日志
     *
     * @param orderId       订单ID
     * @param paymentId     支付ID
     * @param paymentAmount 支付金额
     * @return 是否处理成功
     */
    boolean handlePaymentSuccess(Long orderId, Long paymentId, java.math.BigDecimal paymentAmount);

    /**
     * 取消订单
     * <p>
     * 流程：
     * 1. 检查订单状态是否允许取消
     * 2. 更新订单状态为已取消
     * 3. 记录取消时间和原因
     * 4. 发送库存回滚消息
     * 5. 如果已支付需要发起退款流程
     *
     * @param orderId      订单ID
     * @param cancelReason 取消原因
     * @param operatorId   操作人ID
     * @return 是否取消成功
     */
    boolean cancelOrder(Long orderId, String cancelReason, Long operatorId);

    /**
     * 商家发货
     * <p>
     * 流程：
     * 1. 检查订单状态是否为已支付
     * 2. 更新订单状态为已发货
     * 3. 记录发货时间
     * 4. 发送订单发货消息
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID
     * @return 是否发货成功
     */
    boolean shipOrder(Long orderId, Long operatorId);

    /**
     * 确认收货/完成订单
     * <p>
     * 流程：
     * 1. 检查订单状态是否为已发货
     * 2. 更新订单状态为已完成
     * 3. 记录完成时间
     * 4. 发送订单完成消息
     * 5. 可选：触发评价、积分等后续流程
     *
     * @param orderId    订单ID
     * @param operatorId 操作人ID（用户ID或管理员ID）
     * @return 是否完成成功
     */
    boolean completeOrder(Long orderId, Long operatorId);

    /**
     * 检查订单状态
     *
     * @param orderId 订单ID
     * @return 订单状态
     */
    OrderStatusEnum checkOrderStatus(Long orderId);

    /**
     * 获取订单详情（带锁）
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDTO getOrderWithLock(Long orderId);

    /**
     * 处理支付失败事件
     * <p>
     * 流程：
     * 1. 检查订单状态
     * 2. 如果订单仍在待支付状态，可以保持不变或设置为支付失败
     * 3. 发送库存回滚消息（如果已预扣减）
     * 4. 记录支付失败日志
     *
     * @param orderId    订单ID
     * @param paymentId  支付ID
     * @param failReason 失败原因
     * @return 是否处理成功
     */
    boolean handlePaymentFailed(Long orderId, Long paymentId, String failReason);

    /**
     * 处理库存不足事件
     * <p>
     * 流程：
     * 1. 自动取消订单
     * 2. 通知用户库存不足
     * 3. 记录取消原因
     *
     * @param orderId    订单ID
     * @param productIds 库存不足的商品ID列表
     * @return 是否处理成功
     */
    boolean handleStockShortage(Long orderId, java.util.List<Long> productIds);
}

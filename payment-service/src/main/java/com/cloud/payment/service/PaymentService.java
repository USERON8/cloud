package com.cloud.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
import com.cloud.common.domain.event.payment.PaymentRecordCreateEvent;
import com.cloud.payment.module.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author what's up
 * @description 针对表【payment(支付主表)】的数据库操作Service
 * @createDate 2025-08-17 20:53:31
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 检查支付记录是否存在
     *
     * @param orderId 订单ID
     * @return 是否存在
     */
    boolean isPaymentRecordExists(Long orderId);

    /**
     * 创建支付记录
     *
     * @param event 订单创建事件
     * @return 是否成功
     */
    boolean createPaymentRecord(OrderCreatedEvent event);

    /**
     * 创建支付记录
     *
     * @param event 支付记录创建事件
     * @return 是否成功
     */
    boolean createPaymentRecord(PaymentRecordCreateEvent event);

    /**
     * 分页查询支付列表
     *
     * @param page    页码
     * @param size    每页数量
     * @param userId  用户ID
     * @param status  支付状态
     * @param channel 支付渠道
     * @return 分页结果
     */
    Page<PaymentDTO> getPaymentsPage(Integer page, Integer size, Long userId, Integer status, Integer channel);

    /**
     * 根据支付ID查询支付信息
     *
     * @param id 支付ID
     * @return 支付信息
     */
    PaymentDTO getPaymentById(Long id);

    /**
     * 创建支付订单
     *
     * @param paymentDTO 支付信息
     * @return 支付ID
     */
    Long createPayment(PaymentDTO paymentDTO);

    /**
     * 更新支付信息
     *
     * @param paymentDTO 支付信息
     * @return 是否成功
     */
    Boolean updatePayment(PaymentDTO paymentDTO);

    /**
     * 删除支付记录
     *
     * @param id 支付ID
     * @return 是否成功
     */
    Boolean deletePayment(Long id);

    /**
     * 处理支付成功
     *
     * @param id 支付ID
     * @return 是否成功
     */
    Boolean processPaymentSuccess(Long id);

    /**
     * 处理支付失败
     *
     * @param id         支付ID
     * @param failReason 失败原因
     * @return 是否成功
     */
    Boolean processPaymentFailed(Long id, String failReason);

    /**
     * 处理退款
     *
     * @param id           支付ID
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 是否成功
     */
    Boolean processRefund(Long id, BigDecimal refundAmount, String refundReason);

    /**
     * 根据订单ID查询支付信息
     *
     * @param orderId 订单ID
     * @return 支付信息
     */
    PaymentDTO getPaymentByOrderId(Long orderId);

    /**
     * 支付风控检查
     *
     * @param userId        用户ID
     * @param amount        支付金额
     * @param paymentMethod 支付方式
     * @return 是否通过
     */
    Boolean riskCheck(Long userId, BigDecimal amount, String paymentMethod);

    /**
     * 更新支付状态
     *
     * @param id     支付ID
     * @param status 支付状态
     * @param remark 备注
     * @return 是否成功
     */
    Boolean updatePaymentStatus(Long id, Integer status, String remark);

    /**
     * 获取支付状态
     *
     * @param id 支付ID
     * @return 支付状态
     */
    Integer getPaymentStatus(Long id);

    /**
     * 验证支付金额
     *
     * @param id     支付ID
     * @param amount 金额
     * @return 是否匹配
     */
    Boolean validatePaymentAmount(Long id, BigDecimal amount);

    /**
     * 获取用户支付统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getUserPaymentStats(Long userId);
}

package com.cloud.payment.service;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.payment.module.dto.PaymentOperationResult;

import java.math.BigDecimal;

/**
 * 支付锁服务接口
 * 提供基于分布式锁的支付幂等操作，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface PaymentLockService {

    /**
     * 安全创建支付记录 - 使用分布式锁和幂等处理
     * 基于traceId确保相同请求只创建一次支付记录
     *
     * @param orderId    订单ID
     * @param userId     用户ID
     * @param amount     支付金额
     * @param channel    支付渠道
     * @param traceId    跟踪ID（幂等标识）
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    PaymentOperationResult safeCreatePayment(Long orderId, Long userId, BigDecimal amount,
                                             Integer channel, String traceId,
                                             Long operatorId, String remark);

    /**
     * 安全支付成功处理 - 使用分布式锁保护
     * 从待支付状态转换为成功状态
     *
     * @param paymentId     支付ID
     * @param transactionId 第三方流水号
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
    PaymentOperationResult safePaymentSuccess(Long paymentId, String transactionId,
                                              Long operatorId, String remark);

    /**
     * 安全支付失败处理 - 使用分布式锁保护
     * 从待支付状态转换为失败状态
     *
     * @param paymentId     支付ID
     * @param failureReason 失败原因
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
    PaymentOperationResult safePaymentFailed(Long paymentId, String failureReason,
                                             Long operatorId, String remark);

    /**
     * 安全退款处理 - 使用分布式锁保护
     * 从成功状态转换为已退款状态
     *
     * @param paymentId           支付ID
     * @param refundTransactionId 退款流水号
     * @param operatorId          操作人ID
     * @param remark              备注
     * @return 操作结果
     */
    PaymentOperationResult safeRefundPayment(Long paymentId, String refundTransactionId,
                                             Long operatorId, String remark);

    /**
     * 幂等性检查 - 根据跟踪ID查询支付记录
     * 用于判断是否已经处理过相同的支付请求
     *
     * @param traceId 跟踪ID
     * @return 支付记录，如果不存在返回null
     */
    PaymentDTO checkIdempotency(String traceId);

    /**
     * 获取支付信息 - 使用分布式锁保护
     * 确保获取到的支付信息是最新的
     *
     * @param paymentId 支付ID
     * @return 支付信息
     */
    PaymentDTO getPaymentWithLock(Long paymentId);

    /**
     * 批量支付状态检查 - 使用分布式锁保护
     * 用于批量查询支付状态，确保数据一致性
     *
     * @param paymentIds 支付ID列表
     * @return 支付记录列表
     */
    java.util.List<PaymentDTO> batchGetPaymentStatus(java.util.List<Long> paymentIds);

    /**
     * 支付重试处理 - 使用分布式锁保护
     * 对失败的支付进行重试处理
     *
     * @param paymentId        支付ID
     * @param newTransactionId 新的第三方流水号
     * @param operatorId       操作人ID
     * @param remark           备注
     * @return 操作结果
     */
    PaymentOperationResult safeRetryPayment(Long paymentId, String newTransactionId,
                                            Long operatorId, String remark);
}

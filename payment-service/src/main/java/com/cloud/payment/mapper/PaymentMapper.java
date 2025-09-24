package com.cloud.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.payment.module.entity.Payment;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 支付Mapper接口
 * 提供支付相关的数据库操作，包括幂等处理的条件更新SQL
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface PaymentMapper extends BaseMapper<Payment> {

    // ==================== 支付幂等处理条件更新SQL（并发安全） ====================

    /**
     * 条件更新支付状态 - 从待支付到成功
     * 确保只有待支付状态的支付记录才能更新为成功
     *
     * @param paymentId     支付ID
     * @param transactionId 第三方流水号
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToSuccess(@Param("paymentId") Long paymentId,
                              @Param("transactionId") String transactionId);

    /**
     * 条件更新支付状态 - 从待支付到失败
     * 确保只有待支付状态的支付记录才能更新为失败
     *
     * @param paymentId     支付ID
     * @param failureReason 失败原因
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToFailed(@Param("paymentId") Long paymentId,
                             @Param("failureReason") String failureReason);

    /**
     * 条件更新支付状态 - 从成功到已退款
     * 确保只有成功状态的支付记录才能更新为已退款
     *
     * @param paymentId           支付ID
     * @param refundTransactionId 退款流水号
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToRefunded(@Param("paymentId") Long paymentId,
                               @Param("refundTransactionId") String refundTransactionId);

    /**
     * 根据跟踪ID查询支付记录（幂等性检查）
     *
     * @param traceId 跟踪ID
     * @return 支付记录
     */
    Payment selectByTraceId(@Param("traceId") String traceId);

    /**
     * 根据订单ID和用户ID查询支付记录
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 支付记录
     */
    Payment selectByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 根据支付ID查询支付记录（加行锁）
     * 用于需要加锁查询的场景
     *
     * @param paymentId 支付ID
     * @return 支付记录
     */
    Payment selectByIdForUpdate(@Param("paymentId") Long paymentId);

    /**
     * 幂等创建支付记录
     * 基于traceId的唯一约束，确保相同traceId只能创建一次
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @param amount  支付金额
     * @param channel 支付渠道
     * @param traceId 跟踪ID
     * @return 影响行数，0表示已存在相同traceId的记录
     */
    int insertPaymentIdempotent(@Param("orderId") Long orderId,
                                @Param("userId") Long userId,
                                @Param("amount") BigDecimal amount,
                                @Param("channel") Integer channel,
                                @Param("traceId") String traceId);

    /**
     * 批量查询支付记录状态
     * 用于批量状态检查
     *
     * @param paymentIds 支付ID列表
     * @return 支付记录列表
     */
    java.util.List<Payment> selectBatchByIds(@Param("paymentIds") java.util.List<Long> paymentIds);
}





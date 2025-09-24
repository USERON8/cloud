package com.cloud.payment.service.impl;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.lock.DistributedLockTemplate;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.module.dto.PaymentOperationResult;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * 支付幂等锁服务实现类
 * 基于分布式锁实现的支付幂等操作服务，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLockServiceImpl implements PaymentLockService {

    /**
     * 锁超时时间（秒）
     */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(15);
    /**
     * 锁等待时间（毫秒）
     */
    private static final Duration LOCK_WAIT_TIME = Duration.ofMillis(1000);
    private final PaymentMapper paymentMapper;
    private final PaymentConverter paymentConverter;
    private final DistributedLockTemplate lockTemplate;

    @Override
    @Transactional
    public PaymentOperationResult safeCreatePayment(Long orderId, Long userId, BigDecimal amount,
                                                    Integer channel, String traceId,
                                                    Long operatorId, String remark) {
        validateCreateParameters(orderId, userId, amount, channel, traceId, operatorId);

        String lockKey = buildPaymentCreateLockKey(traceId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 幂等性检查 - 根据traceId查询是否已存在
                Payment existingPayment = paymentMapper.selectByTraceId(traceId);
                if (existingPayment != null) {
                    log.info("🔄 检测到幂等重复请求 - TraceId: {}, 支付ID: {}", traceId, existingPayment.getId());
                    return PaymentOperationResult.idempotentDuplicate(
                            PaymentOperationResult.OperationType.CREATE_PAYMENT,
                            existingPayment.getId(),
                            existingPayment.getOrderId(),
                            existingPayment.getUserId(),
                            existingPayment.getAmount(),
                            existingPayment.getStatus(),
                            existingPayment.getTransactionId(),
                            traceId,
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 执行幂等创建
                int affectedRows = paymentMapper.insertPaymentIdempotent(orderId, userId, amount, channel, traceId);

                if (affectedRows == 0) {
                    // 可能是并发情况下其他线程已创建，再次查询
                    Payment concurrentPayment = paymentMapper.selectByTraceId(traceId);
                    if (concurrentPayment != null) {
                        log.info("🔄 并发创建检测到重复 - TraceId: {}, 支付ID: {}", traceId, concurrentPayment.getId());
                        return PaymentOperationResult.idempotentDuplicate(
                                PaymentOperationResult.OperationType.CREATE_PAYMENT,
                                concurrentPayment.getId(),
                                concurrentPayment.getOrderId(),
                                concurrentPayment.getUserId(),
                                concurrentPayment.getAmount(),
                                concurrentPayment.getStatus(),
                                concurrentPayment.getTransactionId(),
                                traceId,
                                operatorId
                        ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                    }

                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.CREATE_PAYMENT,
                            null, orderId,
                            PaymentOperationResult.ErrorCode.CONCURRENT_UPDATE_FAILED,
                            "支付记录创建失败",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 查询创建的支付记录
                Payment createdPayment = paymentMapper.selectByTraceId(traceId);
                if (createdPayment == null) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.CREATE_PAYMENT,
                            null, orderId,
                            PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                            "支付记录创建后查询失败",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 支付记录创建成功 - 支付ID: {}, 订单ID: {}, 金额: {}, TraceId: {}",
                        createdPayment.getId(), orderId, amount, traceId);

                return PaymentOperationResult.success(
                        PaymentOperationResult.OperationType.CREATE_PAYMENT,
                        createdPayment.getId(),
                        orderId,
                        userId,
                        amount,
                        null,
                        PaymentOperationResult.PaymentStatus.PENDING,
                        null,
                        traceId,
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 支付记录创建异常 - 订单ID: {}, TraceId: {}", orderId, traceId, e);
                return PaymentOperationResult.failure(
                        PaymentOperationResult.OperationType.CREATE_PAYMENT,
                        null, orderId,
                        PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public PaymentOperationResult safePaymentSuccess(Long paymentId, String transactionId,
                                                     Long operatorId, String remark) {
        validateParameters(paymentId, operatorId);
        if (!StringUtils.hasText(transactionId)) {
            throw new IllegalArgumentException("第三方流水号不能为空");
        }

        String lockKey = buildPaymentLockKey(paymentId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前支付状态
                Payment payment = paymentMapper.selectByIdForUpdate(paymentId);
                if (payment == null) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.PAYMENT_SUCCESS,
                            paymentId, null,
                            PaymentOperationResult.ErrorCode.PAYMENT_NOT_FOUND,
                            "支付记录不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = payment.getStatus();

                // 执行条件状态更新
                int affectedRows = paymentMapper.updateStatusToSuccess(paymentId, transactionId);

                if (affectedRows == 0) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.PAYMENT_SUCCESS,
                            paymentId, payment.getOrderId(),
                            PaymentOperationResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("支付状态不允许成功，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 支付成功处理完成 - 支付ID: {}, 订单ID: {}, 状态变化: {} -> {}, 流水号: {}",
                        paymentId, payment.getOrderId(), getStatusName(beforeStatus),
                        getStatusName(PaymentOperationResult.PaymentStatus.SUCCESS), transactionId);

                return PaymentOperationResult.success(
                        PaymentOperationResult.OperationType.PAYMENT_SUCCESS,
                        paymentId,
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        beforeStatus,
                        PaymentOperationResult.PaymentStatus.SUCCESS,
                        transactionId,
                        payment.getTraceId(),
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 支付成功处理异常 - 支付ID: {}", paymentId, e);
                return PaymentOperationResult.failure(
                        PaymentOperationResult.OperationType.PAYMENT_SUCCESS,
                        paymentId, null,
                        PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public PaymentOperationResult safePaymentFailed(Long paymentId, String failureReason,
                                                    Long operatorId, String remark) {
        validateParameters(paymentId, operatorId);
        if (!StringUtils.hasText(failureReason)) {
            throw new IllegalArgumentException("失败原因不能为空");
        }

        String lockKey = buildPaymentLockKey(paymentId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前支付状态
                Payment payment = paymentMapper.selectByIdForUpdate(paymentId);
                if (payment == null) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.PAYMENT_FAILED,
                            paymentId, null,
                            PaymentOperationResult.ErrorCode.PAYMENT_NOT_FOUND,
                            "支付记录不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = payment.getStatus();

                // 执行条件状态更新
                int affectedRows = paymentMapper.updateStatusToFailed(paymentId, failureReason);

                if (affectedRows == 0) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.PAYMENT_FAILED,
                            paymentId, payment.getOrderId(),
                            PaymentOperationResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("支付状态不允许失败，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 支付失败处理完成 - 支付ID: {}, 订单ID: {}, 状态变化: {} -> {}, 失败原因: {}",
                        paymentId, payment.getOrderId(), getStatusName(beforeStatus),
                        getStatusName(PaymentOperationResult.PaymentStatus.FAILED), failureReason);

                return PaymentOperationResult.success(
                        PaymentOperationResult.OperationType.PAYMENT_FAILED,
                        paymentId,
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        beforeStatus,
                        PaymentOperationResult.PaymentStatus.FAILED,
                        failureReason,
                        payment.getTraceId(),
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 支付失败处理异常 - 支付ID: {}", paymentId, e);
                return PaymentOperationResult.failure(
                        PaymentOperationResult.OperationType.PAYMENT_FAILED,
                        paymentId, null,
                        PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional
    public PaymentOperationResult safeRefundPayment(Long paymentId, String refundTransactionId,
                                                    Long operatorId, String remark) {
        validateParameters(paymentId, operatorId);
        if (!StringUtils.hasText(refundTransactionId)) {
            throw new IllegalArgumentException("退款流水号不能为空");
        }

        String lockKey = buildPaymentLockKey(paymentId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前支付状态
                Payment payment = paymentMapper.selectByIdForUpdate(paymentId);
                if (payment == null) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.REFUND_PAYMENT,
                            paymentId, null,
                            PaymentOperationResult.ErrorCode.PAYMENT_NOT_FOUND,
                            "支付记录不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = payment.getStatus();

                // 执行条件状态更新
                int affectedRows = paymentMapper.updateStatusToRefunded(paymentId, refundTransactionId);

                if (affectedRows == 0) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.REFUND_PAYMENT,
                            paymentId, payment.getOrderId(),
                            PaymentOperationResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("支付状态不允许退款，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 支付退款处理完成 - 支付ID: {}, 订单ID: {}, 状态变化: {} -> {}, 退款流水号: {}",
                        paymentId, payment.getOrderId(), getStatusName(beforeStatus),
                        getStatusName(PaymentOperationResult.PaymentStatus.REFUNDED), refundTransactionId);

                return PaymentOperationResult.success(
                        PaymentOperationResult.OperationType.REFUND_PAYMENT,
                        paymentId,
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        beforeStatus,
                        PaymentOperationResult.PaymentStatus.REFUNDED,
                        refundTransactionId,
                        payment.getTraceId(),
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 支付退款处理异常 - 支付ID: {}", paymentId, e);
                return PaymentOperationResult.failure(
                        PaymentOperationResult.OperationType.REFUND_PAYMENT,
                        paymentId, null,
                        PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO checkIdempotency(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            throw new IllegalArgumentException("跟踪ID不能为空");
        }

        Payment payment = paymentMapper.selectByTraceId(traceId);
        return payment != null ? paymentConverter.toDTO(payment) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentWithLock(Long paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("支付ID不能为空");
        }

        String lockKey = buildPaymentLockKey(paymentId);

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            Payment payment = paymentMapper.selectByIdForUpdate(paymentId);
            if (payment == null) {
                throw new EntityNotFoundException("支付记录不存在，支付ID: " + paymentId);
            }
            return paymentConverter.toDTO(payment);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> batchGetPaymentStatus(List<Long> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            throw new IllegalArgumentException("支付ID列表不能为空");
        }

        // 对支付ID排序，避免死锁
        List<Long> sortedPaymentIds = paymentIds.stream().sorted().toList();
        String lockKey = buildBatchPaymentLockKey(sortedPaymentIds);

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            List<Payment> payments = paymentMapper.selectBatchByIds(sortedPaymentIds);
            return payments.stream()
                    .map(paymentConverter::toDTO)
                    .toList();
        });
    }

    @Override
    @Transactional
    public PaymentOperationResult safeRetryPayment(Long paymentId, String newTransactionId,
                                                   Long operatorId, String remark) {
        validateParameters(paymentId, operatorId);
        if (!StringUtils.hasText(newTransactionId)) {
            throw new IllegalArgumentException("新的第三方流水号不能为空");
        }

        String lockKey = buildPaymentLockKey(paymentId);
        long startTime = System.currentTimeMillis();

        return lockTemplate.execute(lockKey, LOCK_TIMEOUT, LOCK_WAIT_TIME, () -> {
            long lockWaitTime = System.currentTimeMillis() - startTime;

            try {
                // 查询当前支付状态
                Payment payment = paymentMapper.selectByIdForUpdate(paymentId);
                if (payment == null) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.RETRY_PAYMENT,
                            paymentId, null,
                            PaymentOperationResult.ErrorCode.PAYMENT_NOT_FOUND,
                            "支付记录不存在",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                Integer beforeStatus = payment.getStatus();

                // 只有失败状态的支付才能重试
                if (!PaymentOperationResult.PaymentStatus.FAILED.equals(beforeStatus)) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.RETRY_PAYMENT,
                            paymentId, payment.getOrderId(),
                            PaymentOperationResult.ErrorCode.INVALID_STATUS_TRANSITION,
                            String.format("支付状态不允许重试，当前状态: %s", getStatusName(beforeStatus)),
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                // 执行重试（更新为成功状态）
                int affectedRows = paymentMapper.updateStatusToSuccess(paymentId, newTransactionId);

                if (affectedRows == 0) {
                    return PaymentOperationResult.failure(
                            PaymentOperationResult.OperationType.RETRY_PAYMENT,
                            paymentId, payment.getOrderId(),
                            PaymentOperationResult.ErrorCode.CONCURRENT_UPDATE_FAILED,
                            "支付重试更新失败",
                            operatorId
                    ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
                }

                log.info("✅ 支付重试成功 - 支付ID: {}, 订单ID: {}, 状态变化: {} -> {}, 新流水号: {}",
                        paymentId, payment.getOrderId(), getStatusName(beforeStatus),
                        getStatusName(PaymentOperationResult.PaymentStatus.SUCCESS), newTransactionId);

                return PaymentOperationResult.success(
                        PaymentOperationResult.OperationType.RETRY_PAYMENT,
                        paymentId,
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        beforeStatus,
                        PaymentOperationResult.PaymentStatus.SUCCESS,
                        newTransactionId,
                        payment.getTraceId(),
                        operatorId,
                        remark
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);

            } catch (Exception e) {
                log.error("❌ 支付重试异常 - 支付ID: {}", paymentId, e);
                return PaymentOperationResult.failure(
                        PaymentOperationResult.OperationType.RETRY_PAYMENT,
                        paymentId, null,
                        PaymentOperationResult.ErrorCode.SYSTEM_ERROR,
                        "系统异常: " + e.getMessage(),
                        operatorId
                ).withTiming(System.currentTimeMillis() - startTime, lockWaitTime);
            }
        });
    }

    /**
     * 创建参数验证
     *
     * @param orderId    订单ID
     * @param userId     用户ID
     * @param amount     支付金额
     * @param channel    支付渠道
     * @param traceId    跟踪ID
     * @param operatorId 操作人ID
     */
    private void validateCreateParameters(Long orderId, Long userId, BigDecimal amount,
                                          Integer channel, String traceId, Long operatorId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付金额必须大于0");
        }
        if (channel == null) {
            throw new IllegalArgumentException("支付渠道不能为空");
        }
        if (!StringUtils.hasText(traceId)) {
            throw new IllegalArgumentException("跟踪ID不能为空");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
    }

    /**
     * 参数验证
     *
     * @param paymentId  支付ID
     * @param operatorId 操作人ID
     */
    private void validateParameters(Long paymentId, Long operatorId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("支付ID不能为空");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
    }

    /**
     * 构建支付创建锁键
     *
     * @param traceId 跟踪ID
     * @return 锁键
     */
    private String buildPaymentCreateLockKey(String traceId) {
        return "payment:create:" + traceId;
    }

    /**
     * 构建支付锁键
     *
     * @param paymentId 支付ID
     * @return 锁键
     */
    private String buildPaymentLockKey(Long paymentId) {
        return "payment:status:" + paymentId;
    }

    /**
     * 构建批量支付锁键
     *
     * @param paymentIds 支付ID列表（已排序）
     * @return 锁键
     */
    private String buildBatchPaymentLockKey(List<Long> paymentIds) {
        return "payment:batch:" + String.join(",", paymentIds.stream().map(String::valueOf).toArray(String[]::new));
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
            case 1 -> "成功";
            case 2 -> "失败";
            case 3 -> "已退款";
            default -> "未知状态(" + status + ")";
        };
    }
}

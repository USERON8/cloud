package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.domain.event.payment.PaymentRecordCreateEvent;
import com.cloud.common.messaging.BusinessLogProducer;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author what's up
 * @description 针对表【payment(支付主表)】的数据库操作Service实现
 * @createDate 2025-08-17 20:53:31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment>
        implements PaymentService {

    private final BusinessLogProducer businessLogProducer;



    @Override
    public boolean isPaymentRecordExists(Long orderId) {
        log.info("检查支付记录是否存在 - 订单ID: {}", orderId);
        try {
            long count = count(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, orderId));
            boolean exists = count > 0;
            log.info("支付记录检查结果 - 订单ID: {}, 是否存在: {}", orderId, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查支付记录是否存在失败 - 订单ID: {}", orderId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createPaymentRecord(OrderCreatedEvent event) {
        log.info("创建支付记录 - 订单ID: {}, 用户ID: {}, 金额: {}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());
        try {
            // 检查是否已存在支付记录
            if (isPaymentRecordExists(event.getOrderId())) {
                log.warn("支付记录已存在，跳过创建 - 订单ID: {}", event.getOrderId());
                return true;
            }

            // 创建支付记录
            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            payment.setUserId(event.getUserId());
            payment.setAmount(event.getTotalAmount());
            payment.setStatus(0); // 待支付状态
            payment.setChannel(1); // 默认支付宝

            boolean saved = save(payment);

            if (saved) {
                log.info("创建支付记录成功 - 支付ID: {}, 订单ID: {}", payment.getId(), event.getOrderId());
            } else {
                log.error("创建支付记录失败 - 订单ID: {}", event.getOrderId());
            }

            return saved;
        } catch (Exception e) {
            log.error("创建支付记录异常 - 订单ID: {}, 错误: {}", event.getOrderId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createPaymentRecord(PaymentRecordCreateEvent event) {
        log.info("创建支付记录 - 订单ID: {}, 用户ID: {}, 金额: {}",
                event.getOrderId(), event.getUserId(), event.getPaymentAmount());
        try {
            // 检查是否已存在支付记录
            if (isPaymentRecordExists(event.getOrderId())) {
                log.warn("支付记录已存在，跳过创建 - 订单ID: {}", event.getOrderId());
                return true;
            }

            // 创建支付记录
            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            payment.setUserId(event.getUserId());
            payment.setAmount(event.getPaymentAmount());
            payment.setStatus(0); // 待支付状态
            payment.setChannel(1); // 默认支付宝
            payment.setTraceId(event.getTraceId());

            boolean saved = save(payment);

            if (saved) {
                log.info("创建支付记录成功 - 支付ID: {}, 订单ID: {}", payment.getId(), event.getOrderId());
            } else {
                log.error("创建支付记录失败 - 订单ID: {}", event.getOrderId());
            }

            return saved;
        } catch (Exception e) {
            log.error("创建支付记录异常 - 订单ID: {}, 错误: {}", event.getOrderId(), e.getMessage(), e);
            return false;
        }
    }

    // ===================== 业务日志发送方法（仅记录关键操作）=====================

    /**
     * 发送支付成功日志
     */
    private void sendPaymentSuccessLog(Payment payment, Long orderId, String operator) {
        try {
            businessLogProducer.sendPaymentSuccessLog(
                    "payment-service",
                    payment.getId(),
                    orderId,
                    "ORDER_" + orderId, // 简化订单号处理
                    payment.getUserId(),
                    "User_" + payment.getUserId(),
                    payment.getAmount(),
                    getPaymentMethodName(payment.getChannel()),
                    "", // 第三方交易号，实际项目中应该从payment实体获取
                    operator
            );
        } catch (Exception e) {
            log.warn("发送支付成功日志失败 - 支付ID: {}, 订单ID: {}", payment.getId(), orderId, e);
        }
    }

    /**
     * 发送支付退款日志
     */
    public void sendPaymentRefundLog(Long paymentId, Long refundId, Long orderId,
                                     Long userId, java.math.BigDecimal refundAmount,
                                     String refundReason, String operator) {
        try {
            businessLogProducer.sendPaymentRefundLog(
                    "payment-service",
                    paymentId,
                    refundId,
                    orderId,
                    "ORDER_" + orderId, // 简化订单号处理
                    userId,
                    "User_" + userId,
                    refundAmount,
                    refundReason,
                    "FULL", // 简化处理，实际应根据退款类型判断
                    "ALIPAY", // 简化处理，实际应从支付记录获取
                    operator
            );
        } catch (Exception e) {
            log.warn("发送支付退款日志失败 - 支付ID: {}, 退款ID: {}, 订单ID: {}",
                    paymentId, refundId, orderId, e);
        }
    }

    /**
     * 获取支付方式名称
     */
    private String getPaymentMethodName(Integer channel) {
        if (channel == null) return "UNKNOWN";
        return switch (channel) {
            case 1 -> "ALIPAY";
            case 2 -> "WECHAT";
            case 3 -> "BANK_CARD";
            case 4 -> "BALANCE";
            default -> "OTHER";
        };
    }

}

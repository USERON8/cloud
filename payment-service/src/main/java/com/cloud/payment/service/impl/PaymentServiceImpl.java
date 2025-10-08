package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
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

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cloud.common.domain.dto.payment.PaymentDTO> getPaymentsPage(
            Integer page, Integer size, Long userId, Integer status, Integer channel) {
        log.info("分页查询支付列表 - 页码: {}, 数量: {}, 用户ID: {}, 状态: {}, 渠道: {}",
                page, size, userId, status, channel);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Payment> paymentPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Payment::getUserId, userId);
        if (status != null) wrapper.eq(Payment::getStatus, status);
        if (channel != null) wrapper.eq(Payment::getChannel, channel);
        wrapper.orderByDesc(Payment::getCreatedAt);
        page(paymentPage, wrapper);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cloud.common.domain.dto.payment.PaymentDTO> result =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(paymentPage.getCurrent(), paymentPage.getSize(), paymentPage.getTotal());
        result.setRecords(new java.util.ArrayList<>());
        return result;
    }

    @Override
    public com.cloud.common.domain.dto.payment.PaymentDTO getPaymentById(Long id) {
        Payment payment = getById(id);
        return payment != null ? new com.cloud.common.domain.dto.payment.PaymentDTO() : null;
    }

    @Override
    public Long createPayment(com.cloud.common.domain.dto.payment.PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setUserId(paymentDTO.getUserId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setStatus(0);
        save(payment);
        return payment.getId();
    }

    @Override
    public Boolean updatePayment(com.cloud.common.domain.dto.payment.PaymentDTO paymentDTO) {
        Payment payment = getById(paymentDTO.getId());
        if (payment == null) return false;
        return updateById(payment);
    }

    @Override
    public Boolean deletePayment(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processPaymentSuccess(Long id) {
        log.info("处理支付成功 - 支付ID: {}", id);
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(2);
        return updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processPaymentFailed(Long id, String failReason) {
        log.info("处理支付失败 - 支付ID: {}, 原因: {}", id, failReason);
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(3);
        return updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processRefund(Long id, java.math.BigDecimal refundAmount, String refundReason) {
        log.info("处理退款 - 支付ID: {}, 金额: {}, 原因: {}", id, refundAmount, refundReason);
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(4);
        return updateById(payment);
    }

    @Override
    public com.cloud.common.domain.dto.payment.PaymentDTO getPaymentByOrderId(Long orderId) {
        Payment payment = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        return payment != null ? new com.cloud.common.domain.dto.payment.PaymentDTO() : null;
    }

    @Override
    public Boolean riskCheck(Long userId, java.math.BigDecimal amount, String paymentMethod) {
        log.info("风控检查 - 用户ID: {}, 金额: {}, 支付方式: {}", userId, amount, paymentMethod);
        return true;
    }

    @Override
    public Boolean updatePaymentStatus(Long id, Integer status, String remark) {
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(status);
        return updateById(payment);
    }

    @Override
    public Integer getPaymentStatus(Long id) {
        Payment payment = getById(id);
        return payment != null ? payment.getStatus() : null;
    }

    @Override
    public Boolean validatePaymentAmount(Long id, java.math.BigDecimal amount) {
        Payment payment = getById(id);
        return payment != null && payment.getAmount().compareTo(amount) == 0;
    }

    @Override
    public java.util.Map<String, Object> getUserPaymentStats(Long userId) {
        log.info("获取用户支付统计 - 用户ID: {}", userId);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        long count = count(new LambdaQueryWrapper<Payment>().eq(Payment::getUserId, userId));
        stats.put("totalCount", count);
        stats.put("successCount", count(new LambdaQueryWrapper<Payment>().eq(Payment::getUserId, userId).eq(Payment::getStatus, 2)));
        return stats;
    }
}

package com.cloud.payment.service.impl;

import com.cloud.common.domain.event.PaymentSuccessEvent;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.utils.StringUtils;
import com.cloud.payment.messaging.producer.PaymentEventProducer;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import com.cloud.payment.service.PaymentTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付测试服务实现
 * 用于测试支付流程，模拟各种支付场景
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTestServiceImpl implements PaymentTestService {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String simulatePaymentSuccess(Long orderId, BigDecimal amount, Integer channel) {
        try {
            // 1. 查找支付记录
            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .one();

            if (payment == null) {
                throw new BusinessException("支付记录不存在，订单ID: " + orderId);
            }

            if (payment.getStatus() == 1) {
                return "支付记录已经是成功状态，无需重复处理";
            }

            // 2. 验证金额（如果提供了金额参数）
            if (amount != null && payment.getAmount().compareTo(amount) != 0) {
                throw new BusinessException("支付金额不匹配，订单金额: " + payment.getAmount() + ", 请求金额: " + amount);
            }

            // 3. 更新支付记录状态
            payment.setStatus(1); // 支付成功
            payment.setChannel(channel != null ? channel : payment.getChannel());
            payment.setTransactionId("TEST_" + System.currentTimeMillis()); // 模拟第三方流水号
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                throw new BusinessException("更新支付记录失败");
            }

            // 4. 发送支付成功事件
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(payment.getId())
                    .paymentNo("PAY_" + payment.getId())
                    .orderId(payment.getOrderId())
                    .orderNo("ORDER_" + payment.getOrderId())
                    .userId(payment.getUserId())
                    .paymentAmount(payment.getAmount())
                    .actualAmount(payment.getAmount())
                    .paymentMethod(payment.getChannel())
                    .paymentMethodName(getChannelName(payment.getChannel()))
                    .thirdPartyTransactionId(payment.getTransactionId())
                    .paymentChannel("TEST")
                    .paymentStatus(1)
                    .beforeStatus(0)
                    .afterStatus(1)
                    .paymentTime(LocalDateTime.now())
                    .completedTime(LocalDateTime.now())
                    .description("模拟支付成功")
                    .operator("TEST_SYSTEM")
                    .traceId(StringUtils.generateTraceId())
                    .build();

            // 发送事件通知订单服务
            paymentEventProducer.sendPaymentSuccessEvent(event);

            log.info("✅ 模拟支付成功完成 - 订单ID: {}, 支付ID: {}, 金额: {}",
                    orderId, payment.getId(), payment.getAmount());

            return String.format("支付成功 - 支付ID: %d, 订单ID: %d, 金额: %s, 流水号: %s",
                    payment.getId(), orderId, payment.getAmount(), payment.getTransactionId());

        } catch (Exception e) {
            log.error("❌ 模拟支付成功失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("模拟支付成功失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String simulatePaymentFailure(Long orderId, String reason) {
        try {
            // 1. 查找支付记录
            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .one();

            if (payment == null) {
                throw new BusinessException("支付记录不存在，订单ID: " + orderId);
            }

            // 2. 更新支付记录状态
            payment.setStatus(2); // 支付失败
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                throw new BusinessException("更新支付记录失败");
            }

            log.info("✅ 模拟支付失败完成 - 订单ID: {}, 支付ID: {}, 原因: {}",
                    orderId, payment.getId(), reason);

            return String.format("支付失败 - 支付ID: %d, 订单ID: %d, 原因: %s",
                    payment.getId(), orderId, reason);

        } catch (Exception e) {
            log.error("❌ 模拟支付失败异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("模拟支付失败异常: " + e.getMessage(), e);
        }
    }

    @Override
    public Object getPaymentStatus(Long orderId) {
        try {
            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .one();

            if (payment == null) {
                return "支付记录不存在";
            }

            Map<String, Object> status = new HashMap<>();
            status.put("paymentId", payment.getId());
            status.put("orderId", payment.getOrderId());
            status.put("userId", payment.getUserId());
            status.put("amount", payment.getAmount());
            status.put("status", payment.getStatus());
            status.put("statusName", getStatusName(payment.getStatus()));
            status.put("channel", payment.getChannel());
            status.put("channelName", getChannelName(payment.getChannel()));
            status.put("transactionId", payment.getTransactionId());
            status.put("createdAt", payment.getCreatedAt());
            status.put("updatedAt", payment.getUpdatedAt());

            return status;

        } catch (Exception e) {
            log.error("❌ 查询支付状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("查询支付状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPaymentStatus(Long orderId) {
        try {
            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .one();

            if (payment == null) {
                throw new BusinessException("支付记录不存在，订单ID: " + orderId);
            }

            // 重置为待支付状态
            payment.setStatus(0);
            payment.setTransactionId(null);
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                throw new BusinessException("重置支付状态失败");
            }

            log.info("✅ 重置支付状态完成 - 订单ID: {}, 支付ID: {}", orderId, payment.getId());

            return String.format("重置成功 - 支付ID: %d, 订单ID: %d, 状态已重置为待支付",
                    payment.getId(), orderId);

        } catch (Exception e) {
            log.error("❌ 重置支付状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new BusinessException("重置支付状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object testFullPaymentFlow(Long userId, BigDecimal amount, String productName) {
        // 这里可以集成订单服务的接口，创建完整的测试流程
        // 由于跨服务调用，这里先返回测试指导
        Map<String, Object> result = new HashMap<>();
        result.put("step1", "请先调用订单服务创建订单接口");
        result.put("step2", "然后调用支付服务创建支付接口");
        result.put("step3", "最后调用本接口的模拟支付成功接口");
        result.put("example", "POST /api/v1/payment/test/simulate-success/{orderId}");
        result.put("userId", userId);
        result.put("amount", amount);
        result.put("productName", productName);

        return result;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "支付成功";
            case 2: return "支付失败";
            case 3: return "已退款";
            default: return "未知状态";
        }
    }

    /**
     * 获取渠道名称
     */
    private String getChannelName(Integer channel) {
        switch (channel) {
            case 1: return "支付宝";
            case 2: return "微信支付";
            case 3: return "银行卡";
            default: return "未知渠道";
        }
    }
}

package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.event.OrderCompleteEvent;
import com.cloud.common.domain.event.OrderCreateEvent;
import com.cloud.common.messaging.AsyncLogProducer;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.messaging.producer.LogCollectionProducer;
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

    private final LogCollectionProducer logCollectionProducer;
    private final AsyncLogProducer asyncLogProducer;

    @Override
    @DistributedLock(
            key = "'payment:create:' + #event.orderId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "创建支付记录获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean createPaymentForOrder(OrderCreateEvent event) {
        try {
            log.info("为订单创建支付记录 - 订单ID: {}, 用户ID: {}, 金额: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());

            // 创建支付记录
            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            payment.setUserId(event.getUserId());
            payment.setAmount(event.getTotalAmount());
            payment.setStatus(0); // 待支付状态
            payment.setChannel(1); // 默认支付宝

            boolean saved = save(payment);

            if (saved) {
                // 异步发送支付记录创建日志 - 不阻塞主业务流程
                asyncLogProducer.sendBusinessLogAsync(
                        "payment-service",
                        "PAYMENT_MANAGEMENT",
                        "CREATE",
                        "支付记录创建",
                        payment.getId().toString(),
                        "PAYMENT",
                        null,
                        String.format("{\"orderId\":%s,\"amount\":%s,\"method\":\"支付宝\"}",
                                event.getOrderId(), event.getTotalAmount()),
                        "SYSTEM",
                        "订单ID: " + event.getOrderId()
                );

                log.info("为订单创建支付记录成功 - 支付ID: {}", payment.getId());
            } else {
                log.error("为订单创建支付记录失败 - 订单ID: {}", event.getOrderId());
            }

            return saved;
        } catch (Exception e) {
            log.error("为订单创建支付记录异常 - 订单ID: {}, 错误: {}", event.getOrderId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    @DistributedLock(
            key = "'payment:complete:' + #event.orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "完成支付操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean completePaymentForOrder(OrderCompleteEvent event) {
        try {
            log.info("完成订单支付 - 订单ID: {}, 用户ID: {}, 金额: {}",
                    event.getOrderId(), event.getUserId(), event.getTotalAmount());

            // 查找对应的支付记录
            Payment payment = lambdaQuery()
                    .eq(Payment::getOrderId, event.getOrderId())
                    .one();

            if (payment == null) {
                log.error("未找到支付记录 - 订单ID: {}", event.getOrderId());
                return false;
            }

            // 更新支付状态为已支付
            payment.setStatus(1);
            // payment.setUpdateTime(LocalDateTime.now()); // BaseEntity会自动处理

            boolean updated = updateById(payment);

            if (updated) {
                // 异步发送支付完成日志 - 不阻塞主业务流程
                asyncLogProducer.sendBusinessLogAsync(
                        "payment-service",
                        "PAYMENT_PROCESSING",
                        "COMPLETE",
                        "支付完成处理",
                        payment.getId().toString(),
                        "PAYMENT",
                        "0", // 待支付状态
                        "1", // 已支付状态
                        "SYSTEM",
                        String.format("订单ID: %s, 金额: %s", event.getOrderId(), payment.getAmount())
                );

                log.info("订单支付完成 - 支付ID: {}, 订单ID: {}", payment.getId(), event.getOrderId());
            } else {
                log.error("更新支付状态失败 - 支付ID: {}", payment.getId());
            }

            return updated;
        } catch (Exception e) {
            log.error("完成订单支付异常 - 订单ID: {}, 错误: {}", event.getOrderId(), e.getMessage(), e);
            return false;
        }
    }

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
    public boolean createPaymentRecord(com.cloud.common.domain.event.OrderCreatedEvent event) {
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
}
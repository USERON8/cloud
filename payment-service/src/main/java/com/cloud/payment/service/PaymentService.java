package com.cloud.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.event.OrderCompleteEvent;
import com.cloud.common.domain.event.OrderCreateEvent;
import com.cloud.payment.module.entity.Payment;

/**
 * @author what's up
 * @description 针对表【payment(支付主表)】的数据库操作Service
 * @createDate 2025-08-17 20:53:31
 */
public interface PaymentService extends IService<Payment> {

    boolean createPaymentForOrder(OrderCreateEvent event);

    boolean completePaymentForOrder(OrderCompleteEvent event);
}
package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.event.OrderCompleteEvent;
import com.cloud.common.domain.event.OrderCreateEvent;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import org.springframework.stereotype.Service;

/**
 * @author what's up
 * @description 针对表【payment(支付主表)】的数据库操作Service实现
 * @createDate 2025-08-17 20:53:31
 */
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment>
        implements PaymentService {

    @Override
    public boolean createPaymentForOrder(OrderCreateEvent event) {
        //todo 创建支付记录
        return false;
    }

    @Override
    public boolean completePaymentForOrder(OrderCompleteEvent event) {
        //todo 完成支付记录
        return false;
    }
}
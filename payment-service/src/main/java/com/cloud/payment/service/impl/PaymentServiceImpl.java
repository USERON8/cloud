package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import com.cloud.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【payment(支付主表)】的数据库操作Service实现
* @createDate 2025-08-17 20:53:31
*/
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment>
    implements PaymentService{

}





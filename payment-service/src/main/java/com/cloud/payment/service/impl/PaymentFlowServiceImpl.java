package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.payment.mapper.PaymentFlowMapper;
import com.cloud.payment.module.entity.PaymentFlow;
import com.cloud.payment.service.PaymentFlowService;
import org.springframework.stereotype.Service;






@Service
public class PaymentFlowServiceImpl extends ServiceImpl<PaymentFlowMapper, PaymentFlow>
        implements PaymentFlowService {

}





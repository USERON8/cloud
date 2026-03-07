package com.cloud.order.service.support;

import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderRemoteService {

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private PaymentDubboApi paymentDubboApi;

    public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
        return paymentDubboApi.getPaymentOrderByOrderNo(mainOrderNo, subOrderNo);
    }

    public Long createRefund(PaymentRefundCommandDTO command) {
        return paymentDubboApi.createRefund(command);
    }
}

package com.cloud.order.service.support;

import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.remote.RemoteCallSupport;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private PaymentDubboApi paymentDubboApi;

  private final RemoteCallSupport remoteCallSupport;

  public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
    return remoteCallSupport.query(
        "payment-service.getPaymentOrderByOrderNo",
        () -> paymentDubboApi.getPaymentOrderByOrderNo(mainOrderNo, subOrderNo));
  }

  public Long createRefund(PaymentRefundCommandDTO command) {
    return remoteCallSupport.command(
        "payment-service.createRefund", () -> paymentDubboApi.createRefund(command));
  }

  public Boolean cancelRefund(String refundNo, String reason) {
    return remoteCallSupport.command(
        "payment-service.cancelRefund", () -> paymentDubboApi.cancelRefund(refundNo, reason));
  }
}

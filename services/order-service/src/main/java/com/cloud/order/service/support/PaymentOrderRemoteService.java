package com.cloud.order.service.support;

import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private PaymentDubboApi paymentDubboApi;

  public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
    try {
      return paymentDubboApi.getPaymentOrderByOrderNo(mainOrderNo, subOrderNo);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE,
          "payment-service unavailable when querying payment order",
          ex);
    }
  }

  public Long createRefund(PaymentRefundCommandDTO command) {
    try {
      return paymentDubboApi.createRefund(command);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE,
          "payment-service unavailable when creating refund",
          ex);
    }
  }

  public Boolean cancelRefund(String refundNo, String reason) {
    try {
      return paymentDubboApi.cancelRefund(refundNo, reason);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE,
          "payment-service unavailable when cancelling refund",
          ex);
    }
  }
}

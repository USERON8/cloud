package com.cloud.payment.service.support;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private OrderDubboApi orderDubboApi;

  public OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo) {
    try {
      return orderDubboApi.getSubOrderStatus(mainOrderNo, subOrderNo);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE,
          "order-service unavailable when querying sub order status",
          ex);
    }
  }
}

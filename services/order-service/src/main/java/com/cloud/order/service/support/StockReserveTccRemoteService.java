package com.cloud.order.service.support;

import com.cloud.api.stock.StockReserveTccApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.RemoteException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReserveTccRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockReserveTccApi stockReserveTccApi;

  public Boolean tryReserve(StockOperateCommandDTO command) {
    if (command == null) {
      return false;
    }
    try {
      return stockReserveTccApi.tryReserve(
          null,
          command.getOrderNo(),
          command.getSubOrderNo(),
          command.getSkuId(),
          command.getQuantity(),
          command.getReason());
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE,
          "stock-service unavailable when trying reserve",
          ex);
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  private RuntimeException translateException(RuntimeException ex) {
    BusinessException businessException = findBusinessException(ex);
    if (businessException != null) {
      return businessException;
    }
    return new RemoteException(
        ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock reserve tcc failed", ex);
  }

  private BusinessException findBusinessException(Throwable throwable) {
    Throwable cursor = throwable;
    while (cursor != null) {
      if (cursor instanceof BusinessException businessException) {
        return businessException;
      }
      cursor = cursor.getCause();
    }
    return null;
  }
}

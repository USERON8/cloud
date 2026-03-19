package com.cloud.order.service.support;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservationRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockDubboApi stockDubboApi;

  public Boolean reserve(StockOperateCommandDTO command) {
    try {
      return stockDubboApi.reserve(command);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock-service unavailable when reserving", ex);
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean confirm(StockOperateCommandDTO command) {
    try {
      return stockDubboApi.confirm(command);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock-service unavailable when confirming", ex);
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean release(StockOperateCommandDTO command) {
    try {
      return stockDubboApi.release(command);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock-service unavailable when releasing", ex);
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean rollback(StockOperateCommandDTO command) {
    try {
      return stockDubboApi.rollback(command);
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "stock-service unavailable when rolling back", ex);
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  private RuntimeException translateException(RuntimeException ex) {
    BizException BizException = findBusinessException(ex);
    if (BizException != null) {
      String message = normalizeMessage(BizException.getMessage());
      if (isInsufficientStock(message)) {
        return new BizException(ResultCode.STOCK_INSUFFICIENT.getCode(), message, ex);
      }
      if (BizException == ex) {
        return BizException;
      }
      return new BizException(BizException.getCode(), message, ex);
    }

    String message = normalizeMessage(ex.getMessage());
    if (isInsufficientStock(message)) {
      return new BizException(ResultCode.STOCK_INSUFFICIENT.getCode(), message, ex);
    }
    return new RemoteException(ResultCode.REMOTE_SERVICE_UNAVAILABLE, message, ex);
  }

  private BizException findBusinessException(Throwable throwable) {
    Throwable cursor = throwable;
    while (cursor != null) {
      if (cursor instanceof BizException BizException) {
        return BizException;
      }
      cursor = cursor.getCause();
    }
    return null;
  }

  private String normalizeMessage(String message) {
    if (message == null || message.isBlank()) {
      return "remote stock reservation failed";
    }

    String normalized = message.replace('\r', '\n').trim();
    int newlineIndex = normalized.indexOf('\n');
    if (newlineIndex >= 0) {
      normalized = normalized.substring(0, newlineIndex).trim();
    }

    while (normalized.matches("^[\\w.$]+(?:Exception|Error):\\s+.*$")) {
      normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
    }

    return normalized.isBlank() ? "remote stock reservation failed" : normalized;
  }

  private boolean isInsufficientStock(String message) {
    return message != null && message.toLowerCase().contains("insufficient salable stock");
  }
}

package com.cloud.order.service.support;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.remote.RemoteCallSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservationRemoteService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockDubboApi stockDubboApi;

  private final RemoteCallSupport remoteCallSupport;

  public Boolean preCheck(List<StockOperateCommandDTO> commands) {
    try {
      return remoteCallSupport.query(
          "stock-service.preCheck", () -> stockDubboApi.preCheck(commands));
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean reserve(StockOperateCommandDTO command) {
    try {
      return remoteCallSupport.command(
          "stock-service.reserve", () -> stockDubboApi.reserve(command));
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean confirm(StockOperateCommandDTO command) {
    try {
      return remoteCallSupport.command(
          "stock-service.confirm", () -> stockDubboApi.confirm(command));
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean release(StockOperateCommandDTO command) {
    try {
      return remoteCallSupport.command(
          "stock-service.release", () -> stockDubboApi.release(command));
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  public Boolean rollback(StockOperateCommandDTO command) {
    try {
      return remoteCallSupport.command(
          "stock-service.rollback", () -> stockDubboApi.rollback(command));
    } catch (RuntimeException ex) {
      throw translateException(ex);
    }
  }

  private RuntimeException translateException(RuntimeException ex) {
    BizException bizException = findBizException(ex);
    if (bizException != null) {
      String message = normalizeMessage(bizException.getMessage());
      if (isInsufficientStock(message)) {
        return new BizException(ResultCode.STOCK_INSUFFICIENT.getCode(), message, ex);
      }
      if (bizException == ex) {
        return bizException;
      }
      return new BizException(bizException.getCode(), message, ex);
    }

    String message = normalizeMessage(ex.getMessage());
    if (isInsufficientStock(message)) {
      return new BizException(ResultCode.STOCK_INSUFFICIENT.getCode(), message, ex);
    }
    return new RemoteException(ResultCode.REMOTE_SERVICE_UNAVAILABLE, message, ex);
  }

  private BizException findBizException(Throwable throwable) {
    Throwable cursor = throwable;
    while (cursor != null) {
      if (cursor instanceof BizException bizException) {
        return bizException;
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
    return message != null
        && (message.toLowerCase().contains("insufficient salable stock")
            || message.toLowerCase().contains("insufficient available stock"));
  }
}

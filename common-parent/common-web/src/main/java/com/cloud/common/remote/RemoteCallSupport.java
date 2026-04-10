package com.cloud.common.remote;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteCallSupport {

  @Nullable private final MeterRegistry meterRegistry;

  public <T> T query(String target, Supplier<T> action) {
    return execute("query", target, action, null);
  }

  public <T> T queryOrFallback(
      String target, Supplier<T> action, Function<RemoteException, T> fallback) {
    return execute("query", target, action, fallback);
  }

  public <T> T command(String target, Supplier<T> action) {
    return execute("command", target, action, null);
  }

  private <T> T execute(
      String kind,
      String target,
      Supplier<T> action,
      @Nullable Function<RemoteException, T> fallback) {
    try {
      T result = action.get();
      record(kind, target, "success");
      return result;
    } catch (BizException | SystemException ex) {
      record(kind, target, "business_error");
      throw ex;
    } catch (RemoteException ex) {
      return handleRemoteException(kind, target, ex, fallback);
    } catch (RpcException ex) {
      RemoteException remoteException = translateRpcException(target, ex);
      return handleRemoteException(kind, target, remoteException, fallback);
    } catch (RuntimeException ex) {
      RemoteException remoteException =
          RemoteException.providerError(ResultCode.REMOTE_SERVICE_ERROR, target, ex);
      return handleRemoteException(kind, target, remoteException, fallback);
    }
  }

  private <T> T handleRemoteException(
      String kind,
      String target,
      RemoteException exception,
      @Nullable Function<RemoteException, T> fallback) {
    if ("query".equals(kind) && fallback != null) {
      log.warn(
          "[REMOTE][FALLBACK] kind={} target={} reason={}", kind, target, exception.getMessage());
      record(kind, target, "fallback");
      return fallback.apply(exception);
    }
    record(kind, target, "failure");
    throw exception;
  }

  private RemoteException translateRpcException(String target, RpcException exception) {
    if (exception.isTimeout()) {
      return RemoteException.timeout(ResultCode.REMOTE_SERVICE_TIMEOUT, target, -1L, exception);
    }
    if (exception.isLimitExceed()) {
      return RemoteException.rejected(ResultCode.REMOTE_SERVICE_REJECTED, target, exception);
    }
    if (exception.isNetwork() || exception.isNoInvokerAvailableAfterFilter()) {
      return RemoteException.unavailable(ResultCode.REMOTE_SERVICE_UNAVAILABLE, target, exception);
    }
    return RemoteException.providerError(ResultCode.REMOTE_SERVICE_ERROR, target, exception);
  }

  private void record(String kind, String target, String outcome) {
    if (meterRegistry == null) {
      return;
    }
    meterRegistry
        .counter("remote.call", "kind", kind, "target", target, "outcome", outcome)
        .increment();
  }
}

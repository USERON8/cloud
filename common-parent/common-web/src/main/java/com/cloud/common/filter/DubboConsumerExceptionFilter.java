package com.cloud.common.filter;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

@Activate(group = CommonConstants.CONSUMER, order = -1000)
@Slf4j
public class DubboConsumerExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        String service = invoker.getInterface().getSimpleName();
        String method = invocation.getMethodName();
        String target = service + "." + method;

        try {
            Result result = invoker.invoke(invocation);
            long elapsed = System.currentTimeMillis() - start;
            warnSlowIfNeeded(service, method, elapsed, invocation);
            if (!result.hasException()) {
                return result;
            }
            Throwable ex = result.getException();
            if (ex instanceof BizException) {
                throw (BizException) ex;
            }
            if (ex instanceof SystemException) {
                throw (SystemException) ex;
            }
            if (ex instanceof RemoteException) {
                throw (RemoteException) ex;
            }
            log.error("[DUBBO][PROVIDER-ERR] {} remoteEx={}", target, ex.getClass().getSimpleName(), ex);
            throw RemoteException.providerError(ResultCode.REMOTE_SERVICE_ERROR, target, ex);
        } catch (RpcException e) {
            long elapsed = System.currentTimeMillis() - start;
            if (e.isTimeout()) {
                log.error(
                        "[DUBBO][TIMEOUT] {} elapsed={}ms timeout={}ms",
                        target,
                        elapsed,
                        resolveTimeoutMs(invocation),
                        e);
                throw RemoteException.timeout(
                        ResultCode.REMOTE_SERVICE_TIMEOUT, target, elapsed, e);
            }
            if (e.isLimitExceed()) {
                log.warn("[DUBBO][REJECTED] {} rejected by provider", target, e);
                throw RemoteException.rejected(ResultCode.REMOTE_SERVICE_REJECTED, target, e);
            }
            if (e.isNetwork() || e.isNoInvokerAvailableAfterFilter()) {
                log.error("[DUBBO][NETWORK] {} unavailable", target, e);
                throw RemoteException.unavailable(
                        ResultCode.REMOTE_SERVICE_UNAVAILABLE, target, e);
            }
            log.error("[DUBBO][RPC-ERR] {} code={}", target, e.getCode(), e);
            throw RemoteException.providerError(ResultCode.REMOTE_SERVICE_ERROR, target, e);
        }
    }

    private void warnSlowIfNeeded(String service, String method, long elapsed, Invocation invocation) {
        long threshold = resolveSlowThresholdMs(invocation);
        if (elapsed > threshold) {
            log.warn("[DUBBO][SLOW] {}.{} elapsed={}ms threshold={}ms", service, method, elapsed, threshold);
        }
    }

    private long resolveSlowThresholdMs(Invocation invocation) {
        return parseLong(invocation.getAttachment("slowThreshold"), 500L);
    }

    private long resolveTimeoutMs(Invocation invocation) {
        return parseLong(invocation.getAttachment(CommonConstants.TIMEOUT_KEY), 1000L);
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}

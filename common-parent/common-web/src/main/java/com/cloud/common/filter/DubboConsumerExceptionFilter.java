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
        try {
            Result result = invoker.invoke(invocation);
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
            String target = invoker.getInterface().getSimpleName() + "." + invocation.getMethodName();
            log.error("[DUBBO-CONSUMER] remote exception in {}", target, ex);
            throw new RemoteException(ResultCode.REMOTE_SERVICE_UNAVAILABLE, target, ex);
        } catch (RpcException e) {
            String target = invoker.getInterface().getSimpleName() + "." + invocation.getMethodName();
            if (e.isTimeout()) {
                throw new RemoteException(ResultCode.REMOTE_SERVICE_TIMEOUT, target + " timeout", e);
            }
            throw new RemoteException(ResultCode.REMOTE_SERVICE_UNAVAILABLE, target + " unavailable", e);
        }
    }
}

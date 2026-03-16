package com.cloud.common.filter;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

@Activate(group = CommonConstants.PROVIDER, order = -1000)
@Slf4j
public class DubboProviderExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result = invoker.invoke(invocation);
        if (!result.hasException()) {
            return result;
        }
        Throwable ex = result.getException();
        if (ex instanceof BizException || ex instanceof SystemException || ex instanceof RemoteException) {
            return result;
        }
        String target = invoker.getInterface().getName() + "." + invocation.getMethodName();
        log.error("[DUBBO-PROVIDER] unhandled exception in {}", target, ex);
        AppResponse appResponse = new AppResponse();
        appResponse.setException(new SystemException(ResultCode.SYSTEM_ERROR, "服务内部错误", ex));
        return appResponse;
    }
}

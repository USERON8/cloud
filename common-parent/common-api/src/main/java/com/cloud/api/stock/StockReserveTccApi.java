package com.cloud.api.stock;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface StockReserveTccApi {

  @TwoPhaseBusinessAction(
      name = "stockReserveTcc",
      commitMethod = "commitReserve",
      rollbackMethod = "cancelReserve")
  boolean tryReserve(
      BusinessActionContext actionContext,
      @BusinessActionContextParameter(paramName = "orderNo") String orderNo,
      @BusinessActionContextParameter(paramName = "subOrderNo") String subOrderNo,
      @BusinessActionContextParameter(paramName = "skuId") Long skuId,
      @BusinessActionContextParameter(paramName = "quantity") Integer quantity,
      @BusinessActionContextParameter(paramName = "reason") String reason);

  boolean commitReserve(BusinessActionContext actionContext);

  boolean cancelReserve(BusinessActionContext actionContext);
}

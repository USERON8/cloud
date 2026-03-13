package com.cloud.stock.tcc;

import com.cloud.api.stock.StockReserveTccApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.stock.service.StockLedgerService;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;



@Component
@DubboService(interfaceClass = StockReserveTccApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class StockReserveTccService implements StockReserveTccApi {

    private final StockLedgerService stockLedgerService;

    @Override
    public boolean tryReserve(BusinessActionContext actionContext,
                              String orderNo,
                              String subOrderNo,
                              Long skuId,
                              Integer quantity,
                              String reason) {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setOrderNo(orderNo);
        command.setSubOrderNo(subOrderNo);
        command.setSkuId(skuId);
        command.setQuantity(quantity);
        command.setReason(reason);
        return Boolean.TRUE.equals(stockLedgerService.reserve(command));
    }

    @Override
    public boolean commitReserve(BusinessActionContext actionContext) {
        StockOperateCommandDTO command = buildCommand(actionContext);
        if (command == null) {
            return true;
        }
        return Boolean.TRUE.equals(stockLedgerService.confirmReservation(command));
    }

    @Override
    public boolean cancelReserve(BusinessActionContext actionContext) {
        StockOperateCommandDTO command = buildCommand(actionContext);
        if (command == null) {
            return true;
        }
        return Boolean.TRUE.equals(stockLedgerService.rollback(command));
    }

    private StockOperateCommandDTO buildCommand(BusinessActionContext actionContext) {
        if (actionContext == null) {
            return null;
        }
        Object subOrderNo = actionContext.getActionContext("subOrderNo");
        Object skuId = actionContext.getActionContext("skuId");
        Object quantity = actionContext.getActionContext("quantity");
        if (subOrderNo == null || skuId == null || quantity == null) {
            return null;
        }
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setOrderNo(asText(actionContext.getActionContext("orderNo")));
        command.setSubOrderNo(String.valueOf(subOrderNo));
        command.setSkuId(asLong(skuId));
        command.setQuantity(asInt(quantity));
        command.setReason(asText(actionContext.getActionContext("reason")));
        return command;
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Long v) {
            return v;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private Integer asInt(Object value) {
        if (value instanceof Integer v) {
            return v;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return value == null ? null : Integer.parseInt(String.valueOf(value));
    }
}

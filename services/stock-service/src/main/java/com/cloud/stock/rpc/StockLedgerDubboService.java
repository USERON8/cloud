package com.cloud.stock.rpc;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = StockDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class StockLedgerDubboService implements StockDubboApi {

  private final StockLedgerService stockLedgerService;

  @Override
  public StockLedgerVO getLedgerBySkuId(Long skuId) {
    return stockLedgerService.getLedgerBySkuId(skuId);
  }

  @Override
  public Boolean reserve(StockOperateCommandDTO command) {
    return stockLedgerService.reserve(command);
  }

  @Override
  public Boolean confirm(StockOperateCommandDTO command) {
    return stockLedgerService.confirm(command);
  }

  @Override
  public Boolean release(StockOperateCommandDTO command) {
    return stockLedgerService.release(command);
  }

  @Override
  public Boolean rollback(StockOperateCommandDTO command) {
    return stockLedgerService.rollback(command);
  }
}

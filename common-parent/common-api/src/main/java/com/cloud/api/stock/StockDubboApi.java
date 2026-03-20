package com.cloud.api.stock;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import java.util.List;

public interface StockDubboApi {

  StockLedgerVO getLedgerBySkuId(Long skuId);

  List<StockLedgerVO> listLedgersBySkuIds(List<Long> skuIds);

  Boolean reserve(StockOperateCommandDTO command);

  Boolean confirm(StockOperateCommandDTO command);

  Boolean release(StockOperateCommandDTO command);

  Boolean rollback(StockOperateCommandDTO command);
}

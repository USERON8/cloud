package com.cloud.stock.service;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;

import java.util.List;

public interface StockLedgerService {

    StockLedgerVO getLedgerBySkuId(Long skuId);

    Boolean reserve(StockOperateCommandDTO command);

    Boolean confirmReservation(StockOperateCommandDTO command);

    Boolean confirm(StockOperateCommandDTO command);

    Boolean release(StockOperateCommandDTO command);

    Boolean rollback(StockOperateCommandDTO command);

    Boolean rollbackBatch(List<StockOperateCommandDTO> commands);
}

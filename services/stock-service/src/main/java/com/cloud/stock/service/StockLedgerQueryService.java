package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.vo.stock.StockLedgerVO;

public interface StockLedgerQueryService {

  Page<StockLedgerVO> pageLowStockLedgers(long pageIndex, int pageSize);

  Page<StockLedgerVO> pageActiveLedgers(long pageIndex, int pageSize);
}

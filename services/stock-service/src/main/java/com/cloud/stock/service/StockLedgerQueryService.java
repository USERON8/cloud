package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.stock.module.entity.StockLedger;

public interface StockLedgerQueryService {

    Page<StockLedger> pageLowStockLedgers(long pageIndex, int pageSize);

    Page<StockLedger> pageActiveLedgers(long pageIndex, int pageSize);
}

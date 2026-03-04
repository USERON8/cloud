package com.cloud.stock.v2.service;

import com.cloud.stock.v2.entity.StockLedgerV2;
import com.cloud.stock.v2.entity.StockReservationV2;

public interface StockV2Service {
    StockLedgerV2 createLedger(StockLedgerV2 ledger);
    StockReservationV2 reserve(String mainOrderNo, String subOrderNo, Long skuId, Integer qty);
    StockLedgerV2 confirm(String subOrderNo, Long skuId, Integer qty);
    StockLedgerV2 release(String subOrderNo, Long skuId, Integer qty, String reason);
}


package com.cloud.stock.service.impl;

import com.cloud.stock.mapper.StockTxnMapper;
import com.cloud.stock.module.entity.StockTxn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockTxnAsyncWriter {

  private final StockTxnMapper stockTxnMapper;

  public void write(StockTxn txn) {
    stockTxnMapper.insert(txn);
  }
}

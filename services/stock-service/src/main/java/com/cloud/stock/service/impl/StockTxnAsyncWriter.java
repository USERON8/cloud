package com.cloud.stock.service.impl;

import com.cloud.stock.mapper.StockTxnMapper;
import com.cloud.stock.module.entity.StockTxn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockTxnAsyncWriter {

  private final StockTxnMapper stockTxnMapper;

  @Async("stockOperationExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void write(StockTxn txn) {
    try {
      stockTxnMapper.insert(txn);
    } catch (RuntimeException ex) {
      log.warn(
          "write stock txn failed, subOrderNo={}, txnType={}",
          txn.getSubOrderNo(),
          txn.getTxnType(),
          ex);
    }
  }
}

package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.mapper.StockSegmentMapper;
import com.cloud.stock.service.StockLedgerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockLedgerQueryServiceImpl implements StockLedgerQueryService {

  private final StockSegmentMapper stockSegmentMapper;

  @Override
  public Page<StockLedgerVO> pageLowStockLedgers(long pageIndex, int pageSize) {
    return (Page<StockLedgerVO>)
        stockSegmentMapper.pageLowStockLedgers(new Page<>(pageIndex, pageSize));
  }

  @Override
  public Page<StockLedgerVO> pageActiveLedgers(long pageIndex, int pageSize) {
    return (Page<StockLedgerVO>)
        stockSegmentMapper.pageActiveLedgers(new Page<>(pageIndex, pageSize));
  }
}

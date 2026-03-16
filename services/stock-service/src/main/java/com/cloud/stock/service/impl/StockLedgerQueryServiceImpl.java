package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.service.StockLedgerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockLedgerQueryServiceImpl implements StockLedgerQueryService {

    private final StockLedgerMapper stockLedgerMapper;

    @Override
    public Page<StockLedger> pageLowStockLedgers(long pageIndex, int pageSize) {
        return stockLedgerMapper.selectPage(
                new Page<>(pageIndex, pageSize),
                new LambdaQueryWrapper<StockLedger>()
                        .eq(StockLedger::getDeleted, 0)
                        .eq(StockLedger::getStatus, 1)
                        .gt(StockLedger::getAlertThreshold, 0)
                        .apply("salable_qty <= alert_threshold")
        );
    }

    @Override
    public Page<StockLedger> pageActiveLedgers(long pageIndex, int pageSize) {
        return stockLedgerMapper.selectPage(
                new Page<>(pageIndex, pageSize),
                new LambdaQueryWrapper<StockLedger>().eq(StockLedger::getDeleted, 0)
        );
    }
}

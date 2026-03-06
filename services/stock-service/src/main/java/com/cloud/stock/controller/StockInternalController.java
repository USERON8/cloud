package com.cloud.stock.controller;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/stock")
@RequiredArgsConstructor
public class StockInternalController implements StockDubboApi {

    private final StockLedgerService stockLedgerService;

    @Override
    @GetMapping("/ledger/{skuId}")
    public StockLedgerVO getLedgerBySkuId(@PathVariable("skuId") Long skuId) {
        return stockLedgerService.getLedgerBySkuId(skuId);
    }

    @Override
    @PostMapping("/reserve")
    public Boolean reserve(@RequestBody StockOperateCommandDTO command) {
        return stockLedgerService.reserve(command);
    }

    @Override
    @PostMapping("/confirm")
    public Boolean confirm(@RequestBody StockOperateCommandDTO command) {
        return stockLedgerService.confirm(command);
    }

    @Override
    @PostMapping("/release")
    public Boolean release(@RequestBody StockOperateCommandDTO command) {
        return stockLedgerService.release(command);
    }

    @Override
    @PostMapping("/rollback")
    public Boolean rollback(@RequestBody StockOperateCommandDTO command) {
        return stockLedgerService.rollback(command);
    }
}


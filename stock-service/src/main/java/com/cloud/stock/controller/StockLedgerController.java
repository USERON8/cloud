package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/stocks")
@RequiredArgsConstructor
public class StockLedgerController {

    private final StockLedgerService stockLedgerService;

    @GetMapping("/ledger/{skuId}")
    public Result<StockLedgerVO> getLedger(@PathVariable Long skuId) {
        return Result.success(stockLedgerService.getLedgerBySkuId(skuId));
    }

    @PostMapping("/reserve")
    public Result<Boolean> reserve(@Valid @RequestBody StockOperateCommandDTO command) {
        return Result.success(stockLedgerService.reserve(command));
    }

    @PostMapping("/confirm")
    public Result<Boolean> confirm(@Valid @RequestBody StockOperateCommandDTO command) {
        return Result.success(stockLedgerService.confirm(command));
    }

    @PostMapping("/release")
    public Result<Boolean> release(@Valid @RequestBody StockOperateCommandDTO command) {
        return Result.success(stockLedgerService.release(command));
    }

    @PostMapping("/rollback")
    public Result<Boolean> rollback(@Valid @RequestBody StockOperateCommandDTO command) {
        return Result.success(stockLedgerService.rollback(command));
    }
}

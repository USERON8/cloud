package com.cloud.stock.v2.controller;

import com.cloud.common.result.Result;
import com.cloud.stock.v2.entity.StockLedgerV2;
import com.cloud.stock.v2.entity.StockReservationV2;
import com.cloud.stock.v2.service.StockV2Service;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/stocks")
@RequiredArgsConstructor
public class StockV2Controller {

    private final StockV2Service stockV2Service;

    @PostMapping("/ledger")
    public Result<StockLedgerV2> createLedger(@RequestBody StockLedgerV2 ledger) {
        return Result.success(stockV2Service.createLedger(ledger));
    }

    @PostMapping("/reserve")
    public Result<StockReservationV2> reserve(@RequestBody ReserveRequest request) {
        return Result.success(stockV2Service.reserve(
                request.getMainOrderNo(),
                request.getSubOrderNo(),
                request.getSkuId(),
                request.getQty()));
    }

    @PostMapping("/confirm")
    public Result<StockLedgerV2> confirm(@RequestBody StockActionRequest request) {
        return Result.success(stockV2Service.confirm(request.getSubOrderNo(), request.getSkuId(), request.getQty()));
    }

    @PostMapping("/release")
    public Result<StockLedgerV2> release(@RequestBody StockActionRequest request) {
        return Result.success(stockV2Service.release(request.getSubOrderNo(), request.getSkuId(), request.getQty(), request.getReason()));
    }

    @Data
    public static class ReserveRequest {
        private String mainOrderNo;
        private String subOrderNo;
        private Long skuId;
        private Integer qty;
    }

    @Data
    public static class StockActionRequest {
        private String subOrderNo;
        private Long skuId;
        private Integer qty;
        private String reason;
    }
}


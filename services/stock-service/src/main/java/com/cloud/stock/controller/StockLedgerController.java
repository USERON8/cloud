package com.cloud.stock.controller;

import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock API", description = "Stock reservation and ledger APIs")
public class StockLedgerController {

  private final StockLedgerService stockLedgerService;

  @GetMapping("/ledger/{skuId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get stock ledger by sku")
  public Result<StockLedgerVO> getLedger(@PathVariable Long skuId) {
    return Result.success(stockLedgerService.getLedgerBySkuId(skuId));
  }
}

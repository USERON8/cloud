package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock API", description = "Stock reservation and ledger APIs")
public class StockLedgerController {

  private final StockLedgerService stockLedgerService;

  @GetMapping("/ledger/{skuId}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get stock ledger by sku")
  public Result<StockLedgerVO> getLedger(@PathVariable Long skuId) {
    return Result.success(stockLedgerService.getLedgerBySkuId(skuId));
  }

  @PostMapping("/reserve")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Reserve stock")
  public Result<Boolean> reserve(@Valid @RequestBody StockOperateCommandDTO command) {
    return Result.success(stockLedgerService.reserve(command));
  }

  @PostMapping("/confirm")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Confirm stock reservation")
  public Result<Boolean> confirm(@Valid @RequestBody StockOperateCommandDTO command) {
    return Result.success(stockLedgerService.confirm(command));
  }

  @PostMapping("/release")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Release reserved stock")
  public Result<Boolean> release(@Valid @RequestBody StockOperateCommandDTO command) {
    return Result.success(stockLedgerService.release(command));
  }

  @PostMapping("/rollback")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Rollback stock reservation")
  public Result<Boolean> rollback(@Valid @RequestBody StockOperateCommandDTO command) {
    return Result.success(stockLedgerService.rollback(command));
  }
}

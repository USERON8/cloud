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
@Tag(name = "库存接口", description = "库存预占和台账接口")
/**
 * 库存后台查询入口。
 *
 * <p>面向管理员的稳定库存台账 API。跨服务治理聚合优先走 StockDubboApi 或 governance-service，不新增 controller 互调。
 */
public class StockLedgerController {

  private final StockLedgerService stockLedgerService;

  @GetMapping("/ledger/{skuId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "按 SKU 查询库存台账")
  public Result<StockLedgerVO> getLedger(@PathVariable Long skuId) {
    return Result.success(stockLedgerService.getLedgerBySkuId(skuId));
  }
}

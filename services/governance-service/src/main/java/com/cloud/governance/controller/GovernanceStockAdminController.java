package com.cloud.governance.controller;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stocks")
@RequiredArgsConstructor
@Tag(name = "Governance Stock Admin API", description = "Governance-owned stock admin APIs")
public class GovernanceStockAdminController {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockDubboApi stockDubboApi;

  private final RemoteCallSupport remoteCallSupport;

  @GetMapping("/ledger/{skuId}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get stock ledger by sku through governance-service")
  public Result<StockLedgerVO> getLedger(@PathVariable Long skuId) {
    return Result.success(
        remoteCallSupport.query(
            "stock-service.governance.getLedgerBySkuId",
            () -> stockDubboApi.getLedgerBySkuId(skuId)));
  }
}

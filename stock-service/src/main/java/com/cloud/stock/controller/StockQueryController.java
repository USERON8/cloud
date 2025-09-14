package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存查询控制器
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock/query")
@RequiredArgsConstructor
@Validated
@Tag(name = "库存查询接口", description = "库存信息查询相关的 RESTful API 接口")
public class StockQueryController {

    private final StockService stockService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取库存信息", description = "根据库存ID获取详细信息")
    public Result<StockDTO> getById(
            @PathVariable
            @Parameter(description = "库存ID", required = true)
            @NotNull(message = "库存ID不能为空")
            @Positive(message = "库存ID必须为正整数") Long id) {
        StockDTO stock = stockService.getStockById(id);
        return Result.success("查询成功", stock);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存信息", description = "根据商品ID获取库存详细信息")
    public Result<StockDTO> getByProductId(
            @PathVariable
            @Parameter(description = "商品ID", required = true)
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId) {
        StockDTO stock = stockService.getStockByProductId(productId);
        return Result.success("查询成功", stock);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量获取库存信息", description = "根据商品ID列表批量获取库存信息")
    public Result<List<StockDTO>> getByProductIds(
            @RequestBody
            @Parameter(description = "商品ID列表", required = true)
            @NotNull(message = "商品ID列表不能为空")
            @NotEmpty(message = "商品ID列表不能为空") List<Long> productIds) {
        List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
        return Result.success("查询成功", stocks);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询库存", description = "根据条件分页查询库存信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public Result<PageResult<StockVO>> page(@RequestBody
                                            @Parameter(description = "分页查询条件")
                                            @Valid @NotNull(message = "分页查询条件不能为空") StockPageDTO pageDTO) {
        PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "检查库存是否充足", description = "检查指定商品的库存是否充足")
    public Result<Boolean> checkStockSufficient(
            @PathVariable
            @Parameter(description = "商品ID", required = true)
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId,
            
            @PathVariable
            @Parameter(description = "所需数量", required = true)
            @NotNull(message = "所需数量不能为空")
            @Positive(message = "所需数量必须为正整数") Integer quantity) {
        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        return Result.success("检查完成", sufficient);
    }
}

package com.cloud.stock.controller;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.stock.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock/query")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "库存查询", description = "库存查询接口")
public class StockQueryController {

    private final StockService stockService;
    private final StockConverter stockConverter = StockConverter.INSTANCE;


    /**
     * 根据商品ID获取库存信息
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存", description = "根据商品ID获取库存信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<StockVO> getStockByProductId(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("开始查询商品库存，商品ID: {}", productId);
        try {
            StockVO stockVO = stockService.getByProductId(productId);
            if (stockVO != null) {
                return Result.success(stockVO);
            } else {
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "未找到该商品的库存信息");
            }
        } catch (Exception e) {
            log.error("查询商品库存失败，商品ID: {}", productId, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询库存列表
     *
     * @param stockPageDTO 分页查询参数
     * @return 库存列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询库存", description = "分页查询库存列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public PageResult<StockVO> queryStockPage(@Parameter(description = "分页查询参数") @RequestBody StockPageDTO stockPageDTO) {
        log.info("开始分页查询库存列表");
        return stockService.getStockPage(stockPageDTO);
    }


    /**
     * 获取库存详情
     *
     * @param id 库存ID
     * @return 库存详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取库存详情", description = "根据ID获取库存详情")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<StockVO> getStockDetail(@Parameter(description = "库存ID") @PathVariable Long id) {
        log.info("开始查询库存详情，ID: {}", id);
        try {
            com.cloud.stock.module.entity.Stock stock = stockService.getById(id);
            if (stock != null) {
                StockVO stockVO = stockConverter.toVO(stock);
                return Result.success(stockVO);
            } else {
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "未找到该库存信息");
            }
        } catch (Exception e) {
            log.error("查询库存详情失败，ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "查询失败: " + e.getMessage());
        }
    }
}
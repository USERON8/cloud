package com.cloud.stock.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.dto.StockPageQueryDTO;
import com.cloud.stock.module.entity.Stock;
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

/**
 * 库存查询控制器
 * 提供库存查询相关接口
 *
 * @author cloud
 * @since 1.0.0
 */
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
     * @param currentUserId 当前用户ID
     * @return 库存信息
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存", description = "根据商品ID获取库存信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<StockVO> getStockByProductId(@Parameter(description = "商品ID") @PathVariable Long productId,
                                              @RequestHeader("X-User-ID") String currentUserId) {
        log.info("开始查询商品库存，商品ID: {}，操作人: {}", productId, currentUserId);
        try {
            Stock stock = stockService.getByProductId(productId);
            if (stock != null) {
                StockVO stockVO = stockConverter.toVO(stock);
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
     * @param queryDTO 查询参数
     * @param currentUserId 当前用户ID
     * @return 库存分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询库存列表", description = "分页查询库存列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Page<StockVO>> pageQuery(@RequestBody StockPageQueryDTO queryDTO,
                                          @RequestHeader("X-User-ID") String currentUserId) {
        log.info("开始分页查询库存列表，查询参数: {}，操作人: {}", queryDTO, currentUserId);
        try {
            Page<Stock> pageResult = stockService.pageQuery(queryDTO);
            Page<StockVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            voPage.setRecords(stockConverter.toVOList(pageResult.getRecords()));
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("分页查询库存列表失败，查询参数: {}", queryDTO, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "查询失败: " + e.getMessage());
        }
    }
}
package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import com.cloud.stock.service.impl.StockLogMessageService;
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
 * 库存管理控制器
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "库存管理", description = "库存管理接口")
public class StockManageController {

    private final StockService stockService;
    private final StockConverter stockConverter = StockConverter.INSTANCE;
    private final StockLogMessageService stockLogMessageService;

    @PostMapping("/product/add")
    @Operation(summary = "增加库存类型", description = "为指定商品增加库存类型")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> addStock(@Parameter(description = "库存信息") @RequestBody StockDTO stockDTO) {
        log.info("开始增加库存，商品ID: {}", stockDTO.getProductId());
        Stock stock = stockConverter.toEntity(stockDTO);
        stockService.save(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                0,
                stock.getStockQuantity(),
                stock.getStockQuantity(),
                1, // 1表示增加库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("增加成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新库存类型", description = "根据ID更新库存类型信息")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> updateStock(@Parameter(description = "库存ID") @PathVariable Long id,
                                      @Parameter(description = "库存信息") @RequestBody StockDTO stockDTO) {
        log.info("开始更新库存，ID: {}", id);
        Stock stock = stockConverter.toEntity(stockDTO);
        stock.setId(id);
        stockService.updateById(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                0, // 更新操作的变更前数量暂时设置为0，实际项目中应该查询数据库获取
                0, // 更新操作的变更数量暂时设置为0
                stock.getStockQuantity(),
                3, // 3表示更新库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除库存类型", description = "根据ID删除库存类型信息")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> deleteStock(@Parameter(description = "库存ID") @PathVariable Long id) {
        log.info("开始删除库存，ID: {}", id);
        Stock stock = stockService.getById(id);
        stockService.removeById(id);
        
        // 异步发送消息给日志服务
        if (stock != null) {
            stockLogMessageService.sendStockChangeMessage(
                    stock.getProductId(),
                    stock.getProductName(),
                    stock.getStockQuantity(),
                    -stock.getStockQuantity(),
                    0,
                    4, // 4表示删除库存
                    "system" // 操作人，实际项目中应该从上下文中获取
            );
        }
        
        return Result.success("删除成功");
    }

}
package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock/count")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "库存数量管理", description = "库存数量相关操作接口")
public class StockCountController {
    private final StockService stockService;
    private final StockConverter stockConverter = StockConverter.INSTANCE;
    private final StockLogMessageService stockLogMessageService;

    @PostMapping("/increase/{productId}")
    @Operation(summary = "增加库存", description = "为指定商品增加库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> increaseStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                        @Parameter(description = "增加数量") @RequestParam Integer count) {
        log.info("开始增加库存，商品ID: {}，增加数量: {}", productId, count);
        
        // 获取当前库存信息
        Stock stock = stockService.getById(productId);
        if (stock == null) {
            return Result.error("商品库存不存在");
        }
        
        // 更新库存数量
        stock.setStockQuantity(stock.getStockQuantity() + count);
        stockService.updateById(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                stock.getStockQuantity() - count, // 变更前数量
                count, // 变更数量
                stock.getStockQuantity(), // 变更后数量
                1, // 1表示增加库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("增加库存成功");
    }
    
    @PostMapping("/reduce/{productId}")
    @Operation(summary = "扣减库存", description = "为指定商品扣减库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> reduceStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                      @Parameter(description = "扣减数量") @RequestParam Integer count) {
        log.info("开始扣减库存，商品ID: {}，扣减数量: {}", productId, count);
        
        // 获取当前库存信息
        Stock stock = stockService.getById(productId);
        if (stock == null) {
            return Result.error("商品库存不存在");
        }
        
        // 检查库存是否足够
        if (stock.getStockQuantity() < count) {
            return Result.error("库存不足");
        }
        
        // 更新库存数量
        stock.setStockQuantity(stock.getStockQuantity() - count);
        stockService.updateById(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                stock.getStockQuantity() + count, // 变更前数量
                -count, // 变更数量
                stock.getStockQuantity(), // 变更后数量
                2, // 2表示扣减库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("扣减库存成功");
    }
    
    @PostMapping("/freeze/{productId}")
    @Operation(summary = "冻结库存", description = "为指定商品冻结库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> freezeStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                      @Parameter(description = "冻结数量") @RequestParam Integer count) {
        log.info("开始冻结库存，商品ID: {}，冻结数量: {}", productId, count);
        
        // 获取当前库存信息
        Stock stock = stockService.getById(productId);
        if (stock == null) {
            return Result.error("商品库存不存在");
        }
        
        // 检查库存是否足够
        if (stock.getStockQuantity() < count) {
            return Result.error("库存不足");
        }
        
        // 更新冻结库存数量
        stock.setFrozenQuantity(stock.getFrozenQuantity() + count);
        stockService.updateById(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                stock.getStockQuantity(), // 变更前数量
                count, // 变更数量
                stock.getStockQuantity(), // 变更后数量
                3, // 3表示冻结库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("冻结库存成功");
    }
    
    @PostMapping("/unfreeze/{productId}")
    @Operation(summary = "解冻库存", description = "为指定商品解冻库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> unfreezeStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                        @Parameter(description = "解冻数量") @RequestParam Integer count) {
        log.info("开始解冻库存，商品ID: {}，解冻数量: {}", productId, count);
        
        // 获取当前库存信息
        Stock stock = stockService.getById(productId);
        if (stock == null) {
            return Result.error("商品库存不存在");
        }
        
        // 检查冻结库存是否足够
        if (stock.getFrozenQuantity() < count) {
            return Result.error("冻结库存不足");
        }
        
        // 更新冻结库存数量
        stock.setFrozenQuantity(stock.getFrozenQuantity() - count);
        stockService.updateById(stock);
        
        // 异步发送消息给日志服务
        stockLogMessageService.sendStockChangeMessage(
                stock.getProductId(),
                stock.getProductName(),
                stock.getStockQuantity(), // 变更前数量
                -count, // 变更数量
                stock.getStockQuantity(), // 变更后数量
                4, // 4表示解冻库存
                "system" // 操作人，实际项目中应该从上下文中获取
        );
        
        return Result.success("解冻库存成功");
    }
    
    @PostMapping("/batch")
    @Operation(summary = "批量变更库存", description = "批量变更多个商品的库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> batchChangeStock(@Parameter(description = "库存变更信息列表") @RequestBody List<StockChangeRequest> changeRequests) {
        log.info("开始批量变更库存，变更请求数量: {}", changeRequests.size());
        
        for (StockChangeRequest request : changeRequests) {
            // 获取当前库存信息
            Stock stock = stockService.getById(request.getProductId());
            if (stock == null) {
                log.warn("商品库存不存在，商品ID: {}", request.getProductId());
                continue;
            }
            
            int beforeCount = stock.getStockQuantity();
            int changeCount = 0;
            int afterCount = stock.getStockQuantity();
            
            switch (request.getChangeType()) {
                case 1: // 增加库存
                    stock.setStockQuantity(stock.getStockQuantity() + request.getCount());
                    changeCount = request.getCount();
                    afterCount = stock.getStockQuantity();
                    break;
                case 2: // 扣减库存
                    if (stock.getStockQuantity() < request.getCount()) {
                        log.warn("库存不足，无法扣减，商品ID: {}，当前库存: {}，请求扣减: {}", 
                                request.getProductId(), stock.getStockQuantity(), request.getCount());
                        continue;
                    }
                    stock.setStockQuantity(stock.getStockQuantity() - request.getCount());
                    changeCount = -request.getCount();
                    afterCount = stock.getStockQuantity();
                    break;
                case 3: // 冻结库存
                    if (stock.getStockQuantity() < request.getCount()) {
                        log.warn("库存不足，无法冻结，商品ID: {}，当前库存: {}，请求冻结: {}", 
                                request.getProductId(), stock.getStockQuantity(), request.getCount());
                        continue;
                    }
                    stock.setFrozenQuantity(stock.getFrozenQuantity() + request.getCount());
                    changeCount = request.getCount();
                    break;
                case 4: // 解冻库存
                    if (stock.getFrozenQuantity() < request.getCount()) {
                        log.warn("冻结库存不足，无法解冻，商品ID: {}，当前冻结库存: {}，请求解冻: {}", 
                                request.getProductId(), stock.getFrozenQuantity(), request.getCount());
                        continue;
                    }
                    stock.setFrozenQuantity(stock.getFrozenQuantity() - request.getCount());
                    changeCount = -request.getCount();
                    break;
                default:
                    log.warn("不支持的变更类型: {}", request.getChangeType());
                    continue;
            }
            
            // 更新库存
            stockService.updateById(stock);
            
            // 异步发送消息给日志服务
            stockLogMessageService.sendStockChangeMessage(
                    stock.getProductId(),
                    stock.getProductName(),
                    beforeCount, // 变更前数量
                    changeCount, // 变更数量
                    afterCount, // 变更后数量
                    request.getChangeType(), // 变更类型
                    "system" // 操作人，实际项目中应该从上下文中获取
            );
        }
        
        return Result.success("批量变更库存成功");
    }
    
    /**
     * 库存变更请求DTO
     */
    @Data
    @Schema(description = "库存变更请求")
    public static class StockChangeRequest {
        @Schema(description = "商品ID")
        private Long productId;
        
        @Schema(description = "变更数量")
        private Integer count;
        
        @Schema(description = "变更类型: 1-增加库存 2-扣减库存 3-冻结库存 4-解冻库存")
        private Integer changeType;
    }
}
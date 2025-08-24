package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
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
 * 库存数量管理控制器
 * 提供库存数量变更相关接口
 *
 * @author cloud
 * @since 1.0.0
 */
@RestController
@RequestMapping("/stock/count")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "库存数量管理", description = "库存数量相关操作接口")
public class StockCountController {
    private final StockService stockService;

    /**
     * 增加库存
     *
     * @param productId 商品ID
     * @param count     增加数量
     * @return 操作结果
     */
    @PostMapping("/increase/{productId}")
    @Operation(summary = "增加库存", description = "为指定商品增加库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> increaseStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                        @Parameter(description = "增加数量") @RequestParam Integer count) {
        log.info("开始增加库存，商品ID: {}，增加数量: {}", productId, count);
        stockService.increaseQuantity(productId, count);
        return Result.success("增加库存成功");
    }

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param count     扣减数量
     * @return 操作结果
     */
    @PostMapping("/reduce/{productId}")
    @Operation(summary = "扣减库存", description = "为指定商品扣减库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> reduceStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                      @Parameter(description = "扣减数量") @RequestParam Integer count) {
        log.info("开始扣减库存，商品ID: {}，扣减数量: {}", productId, count);
        stockService.reduceQuantity(productId, count);
        return Result.success("扣减库存成功");
    }

    /**
     * 冻结库存
     *
     * @param productId 商品ID
     * @param count     冻结数量
     * @return 操作结果
     */
    @PostMapping("/freeze/{productId}")
    @Operation(summary = "冻结库存", description = "为指定商品冻结库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> freezeStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                      @Parameter(description = "冻结数量") @RequestParam Integer count) {
        log.info("开始冻结库存，商品ID: {}，冻结数量: {}", productId, count);
        stockService.FrozenQuantity(productId, count);
        return Result.success("冻结库存成功");
    }

    /**
     * 解冻库存
     *
     * @param productId 商品ID
     * @param count     解冻数量
     * @return 操作结果
     */
    @PostMapping("/unfreeze/{productId}")
    @Operation(summary = "解冻库存", description = "为指定商品解冻库存数量")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> unfreezeStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                        @Parameter(description = "解冻数量") @RequestParam Integer count) {
        log.info("开始解冻库存，商品ID: {}，解冻数量: {}", productId, count);
        stockService.unfreezeQuantity(productId, count);
        return Result.success("解冻库存成功");
    }


}
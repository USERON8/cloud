package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.converter.StockConverter;
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
 * 库存管理控制器
 * 提供库存管理相关接口
 *
 * @author cloud
 * @since 1.0.0
 */
@RestController
@RequestMapping("/stock/manage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "库存类型管理", description = "库存类型管理接口")
public class StockManageController {

    private final StockService stockService;
    private final StockConverter stockConverter = StockConverter.INSTANCE;


    /**
     * 增加库存类型
     *
     * @param stockDTO 库存信息
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/add")
    @Operation(summary = "增加库存类型", description = "为指定商品增加库存类型")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> addStock(@Parameter(description = "库存信息") @RequestBody StockDTO stockDTO,
                                   @RequestHeader("X-User-ID") String currentUserId) {
        log.info("开始增加库存，商品ID: {}，操作人: {}", stockDTO.getProductId(), currentUserId);
        Stock stock = stockConverter.toEntity(stockDTO);
        stockService.save(stock);
        return Result.success("增加成功");
    }

    /**
     * 更新库存类型
     *
     * @param id       库存ID
     * @param stockDTO 库存信息
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @PutMapping("update/{id}")
    @Operation(summary = "更新库存类型", description = "根据ID更新库存类型信息")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> updateStock(@Parameter(description = "库存ID") @PathVariable Long id,
                                      @Parameter(description = "库存信息") @RequestBody StockDTO stockDTO,
                                      @RequestHeader("X-User-ID") String currentUserId) {
        log.info("开始更新库存，ID: {}，操作人: {}", id, currentUserId);
        Stock stock = stockConverter.toEntity(stockDTO);
        stock.setId(id);
        stockService.updateById(stock);
        return Result.success("更新成功");
    }

    /**
     * 删除库存类型
     *
     * @param id 库存ID
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @DeleteMapping("delete/{id}")
    @Operation(summary = "删除库存类型", description = "根据ID删除库存类型信息")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> deleteStock(@Parameter(description = "库存ID") @PathVariable Long id,
                                      @RequestHeader("X-User-ID") String currentUserId) {
        log.info("开始删除库存，ID: {}，操作人: {}", id, currentUserId);
        stockService.removeById(id);
        return Result.success("删除成功");
    }

}
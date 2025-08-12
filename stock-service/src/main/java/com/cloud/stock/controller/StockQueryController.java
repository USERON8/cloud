package com.cloud.stock.controller;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock/query")
@RequiredArgsConstructor
@Slf4j
public class StockQueryController {

    private final StockService stockService;


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
    public Result<StockVO> getProductById(@PathVariable Long productId) {
        try {
            log.info("根据商品id查询库存:{}", productId);

            if (productId == null) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "商品ID不能为空");
            }

            StockVO stock = stockService.getByProductId(productId);

            if (stock == null) {
                log.warn("商品库存不存在，productId: {}", productId);
                return Result.error(ResultCode.STOCK_NOT_FOUND);
            }

            return Result.success(stock);
        } catch (Exception e) {
            log.error("根据商品ID查询库存失败, productId: {}", productId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询库存失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询库存
     *
     * @param pageDTO 分页查询条件
     * @return 库存分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询库存", description = "分页查询库存信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<PageResult<StockVO>> pageQuery(@RequestBody StockPageDTO pageDTO) {
        try {
            log.info("分页查询库存，查询条件：{}", pageDTO);

            if (pageDTO == null) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "查询条件不能为空");
            }

            PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询库存失败, pageDTO: {}", pageDTO, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询库存失败: " + e.getMessage());
        }
    }
}

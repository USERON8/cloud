package com.cloud.stock.controller;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.stock.service.StockService;
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
public class StockController {

    private final StockService stockService;


    @GetMapping("/product/{productId}")
    public Result<StockVO> getProductById(@PathVariable Long productId) {
        log.info("根据商品id查询库存:{}", productId);

        StockVO stock = stockService.getByProductId(productId);

        if (stock == null) {
            log.warn("商品库存不存在，productId: {}", productId);
            return Result.error(ResultCode.STOCK_NOT_FOUND);
        }

        return Result.success(stock);
    }

    /**
     * 分页查询库存
     */
    @PostMapping("/page")
    public Result<PageResult<StockVO>> pageQuery(@RequestBody StockPageDTO pageDTO) {
        log.info("分页查询库存，查询条件：{}", pageDTO);

        PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);

        return Result.success(pageResult);
    }
}
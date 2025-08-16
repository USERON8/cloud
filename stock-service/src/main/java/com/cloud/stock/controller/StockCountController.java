package com.cloud.stock.controller;


import com.cloud.common.domain.Result;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock/count")
@RequiredArgsConstructor
@Slf4j
public class StockCountController {
    private final StockService stockService;


    @PostMapping("/reduce/batch")
    public Result<String> reduceStock() {
        log.info("开始扣减库存");

        return Result.success("扣减成功");
    }


}

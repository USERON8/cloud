package com.cloud.api.stock;

import com.cloud.common.domain.dto.StockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@FeignClient(name = "stock-service")
public interface StockFeign {

    /**
     * 根据商品ID获取库存信息
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @GetMapping("/stock/{productId}")
    StockDTO getStockByProductId(@PathVariable("productId") Long productId);

    /**
     * 批量获取库存信息
     *
     * @param productIds 商品ID列表
     * @return 库存信息列表
     */
    @GetMapping("/stock/batch")
    List<String> getStockByProductIds(@RequestParam("productIds") List<Long> productIds);

    /**
     * 异步获取库存信息
     *
     * @param productId 商品ID
     * @return 异步库存信息
     */
    @GetMapping("/stock/async/{productId}")
    CompletableFuture<String> getStockByProductIdAsync(@PathVariable("productId") Long productId);

    /**
     * 更新库存
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否更新成功
     */
    @PutMapping("/stock/{productId}")
    Boolean updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
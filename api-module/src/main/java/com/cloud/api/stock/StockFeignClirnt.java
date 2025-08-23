package com.cloud.api.stock;

import com.cloud.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 库存服务Feign客户端
 */
@FeignClient(name = "stock-service")
public interface StockFeignClirnt {

    /**
     * 查询库存
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @GetMapping("/stocks/{productId}")
    Result<Object> getStock(@PathVariable("productId") Long productId);

    /**
     * 更新库存
     *
     * @param productId 商品ID
     * @param quantity  库存数量
     * @return 更新结果
     */
    @PutMapping("/stocks/{productId}")
    Result<Void> updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
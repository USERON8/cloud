package com.cloud.api.stock;

import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.stock.StockVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 库存服务Feign客户端
 * 提供库存服务的远程调用接口
 *
 * @author what's up
 */
@FeignClient(name = "stock-service", path = "/internal/stock", contextId = "stockFeignClient")
public interface StockFeignClient {

    /**
     * 根据商品ID查询库存
     *
     * @param productId 商品ID
     * @return 库存信息
     */
@GetMapping("/product/{productId}")
    StockVO getStockByProductId(@PathVariable("productId") Long productId);

    /**
     * 更新库存数量
     *
     * @param productId 商品ID
     * @param quantity  库存数量
     * @return 操作结果
     */
@PutMapping("/{productId}")
    OperationResultVO updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
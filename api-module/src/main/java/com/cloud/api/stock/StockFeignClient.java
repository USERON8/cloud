package com.cloud.api.stock;

import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.stock.StockVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;







@FeignClient(name = "stock-service", path = "/internal/stock", contextId = "stockFeignClient")
public interface StockFeignClient {

    





    @GetMapping("/product/{productId}")
    StockVO getStockByProductId(@PathVariable("productId") Long productId);

    






    @PutMapping("/{productId}")
    OperationResultVO updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);
}

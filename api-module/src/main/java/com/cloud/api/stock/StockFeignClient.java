package com.cloud.api.stock;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * 库存服务Feign客户端
 */
@FeignClient(name = "stock-service")
public interface StockFeignClient {
}